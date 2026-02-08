# GremlinConverter
Converts Gremlins from [KurtVelasco's Desktop Gremlin](https://github.com/KurtVelasco/Desktop_Gremlin) on Windows to [linux-desktop-gremlin](https://github.com/iluvgirlswithglasses/linux-desktop-gremlin) by [@iluvgirlswithglasses](https://github.com/iluvgirlswithglasses)

## Showcase
https://github.com/user-attachments/assets/5dd9b373-9ef3-4c1d-a37d-8c2264886e79

You can run this multiple times on the same Gremlin if you choose to customize any sprites/sounds differently. The old files will simply get overridden and this does not change any the files you choose to convert, only copies them to another location.

IMPORTANT: Some Gremlins have their sprites found in both `SpriteSheet/Gremlins/<Gremlin-name>` and `SpriteSheet/Companions/<Gremlin-name>`. In this case make sure to use the sprites from `SpriteSheet/Gremlins/<Gremlin-name>` as the ones in the `Companions` folder typically has less sprites.

## Download
**Requires Java 25**

Check releases and download the .jar file from there.

## Incompatible Gremlins
- Lemon  - this doesn't have an idle.png sprite which this converter doesn't work without.
- Koyuki - this is due to differences in asset structure but this should already be available in the latest version of [linux-desktop-gremlin](https://github.com/iluvgirlswithglasses/linux-desktop-gremlin).