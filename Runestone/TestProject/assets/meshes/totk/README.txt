Ripped by Qw2, but many thanks to:
KillzXGaming and all the contributors to Switch-Toolbox, especially in relation to .mc / .txtg
Watertoon for their MC decompressor
Credit not necessary.

Notes:
 - In the DAE file, the UV maps are separated as alternate meshes (Often the correct texture will be either normal/specular or AO). However, the FBX has all the UVs merged, so you can use that if you'd rather not combine them manually.
 
 - I have included the eye light from the orignal BOTW game, as I could not find it in the textures for TOTK (as of ripping this model). For accuracy I have not put it in the render or in the dae/fbx, but if you want to recreate the eye shine from the game, it is available to use.
 
 - Textures with endings "_a", "_g", "_b", "_r" and the like are split textures as the texture in question stores data in separate channels. When extensions are combined (e.g. "_a_g_b") this means that those channels were identical for that image, i.e. in that case the alpha, green, and blue channel store the same data in that image.