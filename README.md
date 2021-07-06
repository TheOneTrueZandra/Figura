# ***Links and Information***
[![Curseforge](http://cf.way2muchnoise.eu/full_figura_downloads.svg)](https://minecraft.curseforge.com/projects/figura)
[![Modrinth](https://img.shields.io/badge/dynamic/json?color=5da545&label=modrinth&prefix=downloads%20&query=downloads&url=https://api.modrinth.com/api/v1/mod/B3csO4KD&style=flat&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMSAxMSIgd2lkdGg9IjE0LjY2NyIgaGVpZ2h0PSIxNC42NjciICB4bWxuczp2PSJodHRwczovL3ZlY3RhLmlvL25hbm8iPjxkZWZzPjxjbGlwUGF0aCBpZD0iQSI+PHBhdGggZD0iTTAgMGgxMXYxMUgweiIvPjwvY2xpcFBhdGg+PC9kZWZzPjxnIGNsaXAtcGF0aD0idXJsKCNBKSI+PHBhdGggZD0iTTEuMzA5IDcuODU3YTQuNjQgNC42NCAwIDAgMS0uNDYxLTEuMDYzSDBDLjU5MSA5LjIwNiAyLjc5NiAxMSA1LjQyMiAxMWMxLjk4MSAwIDMuNzIyLTEuMDIgNC43MTEtMi41NTZoMGwtLjc1LS4zNDVjLS44NTQgMS4yNjEtMi4zMSAyLjA5Mi0zLjk2MSAyLjA5MmE0Ljc4IDQuNzggMCAwIDEtMy4wMDUtMS4wNTVsMS44MDktMS40NzQuOTg0Ljg0NyAxLjkwNS0xLjAwM0w4LjE3NCA1LjgybC0uMzg0LS43ODYtMS4xMTYuNjM1LS41MTYuNjk0LS42MjYuMjM2LS44NzMtLjM4N2gwbC0uMjEzLS45MS4zNTUtLjU2Ljc4Ny0uMzcuODQ1LS45NTktLjcwMi0uNTEtMS44NzQuNzEzLTEuMzYyIDEuNjUxLjY0NSAxLjA5OC0xLjgzMSAxLjQ5MnptOS42MTQtMS40NEE1LjQ0IDUuNDQgMCAwIDAgMTEgNS41QzExIDIuNDY0IDguNTAxIDAgNS40MjIgMCAyLjc5NiAwIC41OTEgMS43OTQgMCA0LjIwNmguODQ4QzEuNDE5IDIuMjQ1IDMuMjUyLjgwOSA1LjQyMi44MDljMi42MjYgMCA0Ljc1OCAyLjEwMiA0Ljc1OCA0LjY5MSAwIC4xOS0uMDEyLjM3Ni0uMDM0LjU2bC43NzcuMzU3aDB6IiBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGZpbGw9IiM1ZGE0MjYiLz48L2c+PC9zdmc+)](https://modrinth.com/mod/figura)
[![](https://img.shields.io/discord/805969743466332191?color=%234469eb&label=discord&logo=discord)](https://discord.gg/ekHGHcH8Af)


**Figura requires Fabric Loader and Fabric API.**</br>
Figura uses mixins which are a Fabric API implementation. [Download it here](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
</br></br>
## **Featuring Bedrock Avatar Parity**
Figura is dedicated to reimagining and redefining the player's appearance, giving them greater freedom to unfiltered creativity.

Figura is Bedrock Parity, done better—with clientside features and serverside features to introduce incredible possibilities. The sky is the limit with Figura!

An external authentication server processes your avatars, which therefore allows you to share your creations with others for free, no strings attached.

Figura comes with its limitations, however these are made with the best intentions in mind to ensure that others do not either suffer from performance issues or outright crash.

Figura is built with the intent to give creators full creative freedom. This comes in the form of an extremely easy to use creation pipeline, and a verbose and extensive Lua API for reading data from Minecraft. With a filesize limit of just 100kb, avatars can go a long way for something so small.
</br></br>
## ***What's so Great about Figura?***
Notable features for the mod include, but aren't limited to...
<details>
  <summary>❗ What has yet to be added?</summary>
  
*Please note that any mentions of serverside or intentional data sharing have not yet been implemented as of Alpha 6. Figura does not store any personally indentifiable information other than your player UUID in order to handle avatars.*
  
</details>

### **Clientside and Serverside**
  - **Figura gives the user full control** of their avatars clientside and will always work outside of Figura-compatible servers.
  - **Users are required to have the mod installed** to see your avatar, serverside introduces additional features for your fantastic avatars to mess around with.

### **Freedom and Security**
  - **Your skill is the limit**! Figura is built to support users in creating the avatars that they have always wanted to create. Whether it be an EVA Unit or an Autobot!
  - **Security is at the forefront** of the mod, so that you don't have to worry about losing your personally identifiable information.
  - **Sharing your data is an intentional opt-in** and only required for debugging purposes. Figura will **never** use your data for advertising.

### **Game Performance**
  - **Performance settings** are determined by the user. You can set custom permissions for every individual. Lower-end computers can adjust settings to give headroom for their poor CPUs!
  - **Blockbench hierarchy** determines the rendering order for model parts, giving creators the chance to prioritize specific details over others. Quads and Tris on mesh models are limited using the same system.
  - **Scripts are limited** to user-determined instruction counts, ensuring that it is near impossible for random users to destroy your framerate with looped code.
  - **Scripts are sandboxed**, only allowing for basic math libraries and executing very safely guarded Java code. This creates a barrier for malicious scripts. Lua scripting is also multi-threaded, meaning your framerate will likely never drop significantly due to a slow-executing script.


### **Easy as Pie**
  - **No converting** required! You can save your Blockbench Project as-is in the avatar's folder.
  - **Hassle-free Texture Mapping**. You never have to worry about manually UV mapping outside of the designated program! Mapping in Blockbench or any 3D programs will ensure 1:1 mapping in-game.
  - **Hotswapping** is as easy as 1, 2, 3! Changes will appear in-game near-instant!

## ***Plans***
What is planned for the future of Figura?

### **Blockbench Animations**
  - **The ability to manipulate avatars** in any way you'd like! Wave, dance, do whatever you want! Coming soon to Figura.
### **Karma / Score**
  - **A player behaviour system**. Getting reported hurts your karma, while not getting reported rewards you karma. Users will be able to limit avatars based on someone's karma, allowing for a sort of "crowd moderation" system.
### **Avatar Shaders**
  - **Writing personal shaders** for your avatars. This is planned for 1.17.
### **Player Worlds**
  - **Upload a structure file** generated using structure blocks to the Figura server, and allow other players to "visit" those structures in an empty world. These worlds could be used for showing off avatars, or just builds, or anything else.
### **Avatar Stitching**
  - **Stitching parts of avatars** such as backpacks, hats, footwear, glasses, etc. This will be likely introduced post-Model Browser's introduction.
