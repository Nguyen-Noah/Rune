package sandbox.panels

import imgui.ImGui
import imgui.extension.nodeditor.NodeEditor
import imgui.extension.nodeditor.flag.NodeEditorPinKind
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.type.ImBoolean
import imgui.type.ImInt
import imgui.type.ImLong
import sandbox.panels.graph.Graph
import sandbox.panels.graph.GraphNode
import sandbox.panels.graph.TerrainGraphNodeKind

/**
 * ImGui node editor + parameter UI for [Graph]; calls [onRebuild] to recompile terrain in the app layer.
 *
 * Tooltips, combos, and other popups must not run inside [NodeEditor.beginNode] — defer them until after
 * nodes/links are drawn and wrap in [NodeEditor.suspend]/[NodeEditor.resume]. See imgui-node-editor #48.
 */
class TerrainGraphPanel {
    private val ctx = NodeEditor.createEditor()
    private val tmpFloat = FloatArray(1)
    private val tmpIntDrag = IntArray(1)
    private val newNodeKind = ImInt(0)
    private val kindLabels = TerrainGraphNodeKind.entries.map { it.label }.toTypedArray()
    private val noiseTypeLabels = arrayOf("FBM", "Billowed")

    private var pendingKindPopupOpen: Long? = null
    private var activeKindPopupNodeId: Long? = null

    private var pendingNoisePopupOpen: Long? = null
    private var activeNoisePopupNodeId: Long? = null

    fun show(
        open: ImBoolean,
        graph: Graph,
        onRebuild: () -> Unit,
        lastError: () -> String?,
    ) {
        ImGui.setNextWindowSize(520f, 480f, ImGuiCond.Once)
        if (ImGui.begin("Terrain graph", open)) {
            if (ImGui.button("Rebuild terrain")) {
                onRebuild()
            }
            ImGui.sameLine()
            if (ImGui.button("Navigate to content")) {
                NodeEditor.navigateToContent()
            }

            lastError()?.let { err ->
                ImGui.pushStyleColor(ImGuiCol.Text, 1f, 0.35f, 0.35f, 1f)
                ImGui.textWrapped(err)
                ImGui.popStyleColor()
            }

            ImGui.separator()
            ImGui.setNextItemWidth(160f)
            ImGui.combo("##newnodetype", newNodeKind, kindLabels)
            ImGui.sameLine()
            if (ImGui.button("Add node")) {
                graph.createGraphNode(TerrainGraphNodeKind.entries[newNodeKind.get()])
            }

            ImGui.spacing()
            NodeEditor.setCurrentEditor(ctx)
            NodeEditor.begin("Node Editor")

            graph.nodes.values.forEach { node ->
                NodeEditor.beginNode(node.nodeId)
                ImGui.text("${node.kind.label}  (${node.nodeId})")

                NodeEditor.beginPin(node.inputPinId, NodeEditorPinKind.Input)
                ImGui.text("-> In")
                NodeEditor.endPin()

                ImGui.sameLine()

                NodeEditor.beginPin(node.outputPinId, NodeEditorPinKind.Output)
                ImGui.text("Out ->")
                NodeEditor.endPin()

                ImGui.pushID(node.nodeId.toInt())
                drawNodeKindButton(node)
                when (node.kind) {
                    TerrainGraphNodeKind.NOISE -> drawNoiseParams(node)
                    TerrainGraphNodeKind.IDENTITY -> { }
                }
                ImGui.popID()

                NodeEditor.endNode()
            }

            if (NodeEditor.beginCreate()) {
                val a = ImLong()
                val b = ImLong()

                if (NodeEditor.queryNewLink(a, b)) {
                    val source = graph.findByOutput(a.get())
                    val target = graph.findByInput(b.get())
                    if (source != null && target != null && source.outputNodeId != target.nodeId && NodeEditor.acceptNewItem()) {
                        source.outputNodeId = target.nodeId
                    }
                }
                NodeEditor.endCreate()
            }

            var uniqueLinkId: Long = 1
            graph.nodes.values.forEach { node ->
                if (node.outputNodeId != null && graph.nodes.containsKey(node.outputNodeId)) {
                    graph.nodes[node.outputNodeId]?.let {
                        NodeEditor.link(uniqueLinkId++, node.outputPinId, it.inputPinId)
                    }
                }
            }

            NodeEditor.suspend()

            if (pendingKindPopupOpen != null) {
                val id = pendingKindPopupOpen!!
                ImGui.openPopup("tg_kind_$id")
                activeKindPopupNodeId = id
                pendingKindPopupOpen = null
            }
            if (pendingNoisePopupOpen != null) {
                val id = pendingNoisePopupOpen!!
                ImGui.openPopup("tg_noise_$id")
                activeNoisePopupNodeId = id
                pendingNoisePopupOpen = null
            }

            drawDeferredKindPopup(graph)
            drawDeferredNoisePopup(graph)

            NodeEditor.resume()

            NodeEditor.end()
        }
        ImGui.end()
    }

    private fun drawNodeKindButton(node: GraphNode) {
        ImGui.text("Type")
        ImGui.sameLine()
        if (ImGui.button("${node.kind.label}##typebtn_${node.nodeId}")) {
            pendingKindPopupOpen = node.nodeId
        }
    }

    private fun drawNoiseParams(node: GraphNode) {
        tmpIntDrag[0] = node.noiseSeed.toInt()
        if (ImGui.dragInt("Seed", tmpIntDrag, 1f)) {
            node.noiseSeed = tmpIntDrag[0].toLong()
        }

        tmpFloat[0] = node.noiseFrequency
        if (ImGui.dragFloat("Frequency", tmpFloat, 0.0005f, 0.0001f, 0.2f)) {
            node.noiseFrequency = tmpFloat[0]
        }

        tmpIntDrag[0] = node.noiseOctaves
        if (ImGui.dragInt("Octaves", tmpIntDrag, 0.25f, 1, 12)) {
            node.noiseOctaves = tmpIntDrag[0].coerceIn(1, 12)
        }

        tmpFloat[0] = node.noisePersistence
        if (ImGui.dragFloat("Persistence", tmpFloat, 0.01f, 0.01f, 1f)) {
            node.noisePersistence = tmpFloat[0]
        }

        tmpFloat[0] = node.noiseLacunarity
        if (ImGui.dragFloat("Lacunarity", tmpFloat, 0.05f, 1f, 64f)) {
            node.noiseLacunarity = tmpFloat[0]
        }

        tmpFloat[0] = node.noiseHeightScale
        if (ImGui.dragFloat("Height scale", tmpFloat, 0.1f, 0.1f, 200f)) {
            node.noiseHeightScale = tmpFloat[0]
        }

        val noiseLabel = noiseTypeLabels.getOrElse(node.noiseTypeOrdinal.coerceIn(0, noiseTypeLabels.lastIndex)) {
            noiseTypeLabels[0]
        }
        ImGui.text("Noise")
        ImGui.sameLine()
        if (ImGui.button("$noiseLabel##noisebtn_${node.nodeId}")) {
            pendingNoisePopupOpen = node.nodeId
        }
    }

    private fun drawDeferredKindPopup(graph: Graph) {
        val id = activeKindPopupNodeId ?: return
        val node = graph.nodes[id] ?: run {
            activeKindPopupNodeId = null
            return
        }
        if (ImGui.beginPopup("tg_kind_$id")) {
            TerrainGraphNodeKind.entries.forEach { k ->
                if (ImGui.menuItem(k.label, "", node.kind == k)) {
                    node.kind = k
                    ImGui.closeCurrentPopup()
                    activeKindPopupNodeId = null
                }
            }
            ImGui.endPopup()
        } else if (!ImGui.isPopupOpen("tg_kind_$id")) {
            activeKindPopupNodeId = null
        }
    }

    private fun drawDeferredNoisePopup(graph: Graph) {
        val id = activeNoisePopupNodeId ?: return
        val node = graph.nodes[id] ?: run {
            activeNoisePopupNodeId = null
            return
        }
        if (node.kind != TerrainGraphNodeKind.NOISE) {
            activeNoisePopupNodeId = null
            return
        }
        if (ImGui.beginPopup("tg_noise_$id")) {
            noiseTypeLabels.forEachIndexed { i, label ->
                if (ImGui.menuItem(label, "", node.noiseTypeOrdinal == i)) {
                    node.noiseTypeOrdinal = i
                    ImGui.closeCurrentPopup()
                    activeNoisePopupNodeId = null
                }
            }
            ImGui.endPopup()
        } else if (!ImGui.isPopupOpen("tg_noise_$id")) {
            activeNoisePopupNodeId = null
        }
    }
}
