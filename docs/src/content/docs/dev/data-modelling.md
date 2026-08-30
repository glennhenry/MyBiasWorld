---
title: Data Modelling
slug: data-modelling
description: Data Modelling docs
---

MongoDB has 16MB document limit, so for non-growing data, we should separate it into independent collections. The constraints:

- Data can grow indefinitely
- May be large
- Not related

Unless the data is limited, small (although may grow indefinitely), or related, separate them into collections.

- Account would be simple data and system administration. It contains `userId`, `username`, `displayName`, `email`, `hashedPassword`, etc.
- Profile is where everything about user is located. It's basically the `PlayerObjects`, like the game data. It contains `displayName` (duplicated), `bio`, `country`, `birthday`, statistics data, blocked users, fan profile such as `bias`, `favoriteSong`, etc; and game profile such as `level`, `xp`, `coins`, `badges`, `achievements`, etc.
- Growing data like friends, guestbook, scrapbook, and activity will be in a separate collection.
  - Friendship is interesting, it's actually small enough to be embedded. But if we ever need function like 'find all users friend with userB', embedded model will suffer hardly because the only way is scanning every user's documents.
  - Guestbook itself is like another topic-reply-comment system. It would be in separate collection like reply, with `guestbookEntry` being the item. Guestbook's reply would be embedded on each entry, similar to how comments are stored in reply.
  - Scrapbook is rather a simple system. It's essentially a bookmark, so it is just a link to a particular resource of the site. However, it can grow and we may make a chance to how it work, so this is a separate collection.
  - Activity can grow indefinitely, it contains a content (brief text describing what happened), an optional link, and timestamp.

Things like badges should rely on definiton + owned model. Definition is like `badgeId`, `displayName`, `description`, `imgUrl`, while owned model is like `badgeId`, `earnedAt`. This makes badges and achievements small and easy to maintain.

As for definitions, we won't put them in the database. The database should only contain user and server data. Definitions should be hot loaded during startup via external files like JSON.

User and system statistics should be separated with the actual count. For example, getting a reply count of a post shouldn't be a query of 'on replies, FIND topic_id == "xyz" COUNT'

- User statistics can be placed on profile. This contains basically everything from posts count, replies count, likes/dislikes casted, comments, polls joined, etc.
- Server statistics can be placed on `ServerObjects`.
