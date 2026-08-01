# Custom Auction House

## Overview
Custom Auction House is a simple way to sell and buy items from other players using diamonds. Server owners can also configure it to use points instead.

## Features
- Browse listings in a protected auction GUI.
- List the item in your hand with a whole-number price.
- Secure purchases that pay the seller and deliver the item.
- Persistent listings and uncollected seller earnings.
- Diamond or points currency mode.

## How to Use
- Run `/auctionhouse open` to browse available items.
- Hold an item and run `/auctionhouse sell <price>` to list it.
- Click a listing to buy it. The item is delivered immediately.
- Run `/auctionhouse collect` to collect your seller earnings.
- Run `/auctionhouse status` to see the number of active listings.

## Tips & Tricks
Listings use the full item stack in your main hand. Make sure your inventory has room before buying or collecting.

## Known Limitations
Points mode uses the server's built-in points balance for this auction house; it does not automatically connect to an external economy plugin. The GUI displays the first 45 listings at a time.
