# GremlinConverter
Converts Gremlins from [KurtVelasco's Desktop Gremlin](https://github.com/KurtVelasco/Desktop_Gremlin) on Windows to [linux-desktop-gremlin](https://github.com/iluvgirlswithglasses/linux-desktop-gremlin) by [@iluvgirlswithglasses](https://github.com/iluvgirlswithglasses)

## Showcase
https://github.com/user-attachments/assets/5dd9b373-9ef3-4c1d-a37d-8c2264886e79

In case it wasn't clear, this should work for all Gremlins (except Koyuki mentioned below), not just Tamamo Cross.

IMPORTANT: Some Gremlins have their sprites found in both `SpriteSheet/Gremlins/<Gremlin-name>` and `SpriteSheet/Companions/<Gremlin-name>`. In this case make sure to use the sprites from `SpriteSheet/Gremlins/<Gremlin-name>` as the ones in the `Companions` folder typically has less sprites.

## Download
**Requires Java 25**

Check releases and download the .jar file from there.

## Incompatible Gremlins
- Koyuki, this is due to differences in asset structure but [Xgameisdabest's fork](https://github.com/Xgameisdabest/linux-desktop-gremlin/tree/koyuki) contains a converted Koyuki gremlin which you can copy to your .config directory.

Edit: As of posting this currently uses the old gremlin folder structure. Simply create a folder called `koyuki` and inside copy and rename the koyuki folder in the sounds folder from the repo as `sounds` and same for the one in the spritesheet folder as `sprites`. Then put the new `koyuki` folder in `~/linux-desktop-gremlin/gremlins`