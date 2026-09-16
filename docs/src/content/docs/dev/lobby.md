---
title: Lobby
slug: lobby
description: Lobby docs
---

The lobby is the landing page of the platform. It is the central place of where multiple type of contents are aggregated. It's important to enhance the social and lively ambience of lobby.

In a typical social portal application, the home page is generally filled with various contents such as news, mail, weather, stock, shopping, and many more. Depending on the content, some may be static: loaded once, and never updated again unless the page is refreshed. On the other hand, others may be updated lively like a stock ticker, where it's more convinient for it to update itself rather than the user refreshing.

Similarly, MyBiasWorld's lobby can aggregate the content in a semi-live manner. Static contents like the latest cafe posts can be returned directly from the server, while dynamic content like activity feed is fetched once, then updated periodically.

### Events

The lobby has a place to show the platform's activity feed. It contains user's activity like:

- "User X has reached level 5"
- "Post 'X' received 120 views in under a minute"

The activity feed is dynamic. The page should periodically request for new events for liveliness.
