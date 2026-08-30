---
title: Cafe
slug: cafe
description: Cafe docs
---

The cafe is structured like:

```
Cafe
  [Lounge]
    Kep1er Discussion
    K-pop Discussion

  [Bias Corner]
    Yujin's Space
      topic1:
          title: "Oh, Yujin is so pretty..."
          author: "UtokkiForever"
		      content: "I think I have fallen for her..."
          reply1
            author: "ThinkingInXiao"
            content: "Yeah she is!"
			      comments: []
      topic2:
          title: "Yujin's Fancam Collection Help"
          author: "UtokkiForever"
		      content: "I need help finding more fancam of her"
          reply1
            author: "Yujiniee"
            content: "I have a tons! Send me a letter."
			      comments: [
				      author: "ThinkingInXiao"
				      content: "Give me too please"
			      ]
    Xiaoting's Space
      ...

  [Terrace]
    Media
    Games

Cafe
└─ Space
   └─ Section
       └─ Topic
            └─ Reply
                └─ Comments
```

The atomic unit of the cafe system is _topic_. It represents a single forum post. User create a topic with a title and content. Each post within the topic is considered a reply. The author's post itself is not considered as a reply. Each reply may have comments, which is a response directed to a particular reply within a topic.

The forum will be divided into _spaces_, then _sections_, and finally individual topics.

- Space: a group of discussions with similar subject. This is imagined as a real spot in a cafe.
  - e.g., lounge, bias corner, terrace
- Section: discussion subject within a space; a _collection of topics_. This does not relate to any location terminology. A section restrict users to only discuss about the relevant subject.
  - e.g.,
    - Kep1er Discussion, discussion related to Kep1er. Others K-pop group discussion shouldn't belong here.
    - Yujin's Space, discussion related to Yujin; e.g., solo activities, talk directly related to Yujin. Other members' discussions shouldn't belong here unless related or talk about Yujin specifically. If it's more like a group activity, it may belong to Kep1er Discussion instead.
    - Media and Games, off-topic discussion; e.g., other media culture and gaming discussions.
- Topic: a single forum post. They are exclusively within a section.
  - e.g., the topic1 with title "Oh, Yujin is so pretty..."
- Reply: unit of content within a topic.
  - e.g., reply1 in topic1 with content "Yeah she is!"
- Comments: a direct response to a reply in a post.
  - e.g., the content "Give me too please" in topic2

The internal name does not need to reflect the cosmetic name used in the application. e.g., "Yujin's Space" when technically it's a section.

Section and space will be stored in a separate collection in the database from the topics. Technically, opening the cafe page will retrieve the available spaces, group them within all the sections, and fetch the latest topics from each section.

Section and space won't likely be created often, so the server should cache them in memory to avoid repeated DB query. In reality, creation could be once every few months or even never. Though, they should still be stored in the DB instead of hardcoded in the app to avoid administrator editing the code just to create new. This makes it possible to implement a "create new section" feature.

Space and section will be a very tiny collection. We can model it like:

```json
spaces: [
  {
    id: "lounge",
    name: "Lounge",
    order: 0
  },
  {
    id: "bias-corner",
    name: "Bias Corner",
    order: 1
  },
  {
    id: "terrace",
    name: "Terrace (off-topic)",
    order: 2
  }
]
```

Order is a numerical value that will determine the cafe layout display. The `id` here is merely for referential purpose. It isn't shown in the cafe application. User either open the cafe which shows every space and sections within them, or open individual sections to see every topics.

```json
sections: [
  {
    id: "kep1er",
    spaceId: "lounge",
    name: "Kep1er Discussion",
    order: 0
  },
  {
    id: "kpop",
    spaceId: "lounge",
    name: "K-pop Discussion",
    order: 1
  },
  {
    id: "yujin",
    spaceId: "bias-corner",
    name: "Yujin's Space",
    order: 0
  },
  {
    id: "xiaoting",
    spaceId: "bias-corner",
    name: "Xiaoting's Space",
    order: 1
  },
  {
    id: "media",
    spaceId: "terrace",
    name: "Media",
    order: 0
  },
  {
    id: "games",
    spaceId: "terrace",
    name: "Games",
    order: 1
  }
]
```

Order value of each section represents their order on their respective space; e.g., "Xiaoting's Space" is ordered as the second within the "bias_corner" space, preceded by "Yujin's Space" which is the first.

Unlike space, section's `id` will be displayed to users. It will be used as the forum URL. Therefore, the `id` shouldn't contain any special characters. It should also be manually created instead of produced from section's name. This avoid long URL just because the section's name is long.

For example, clicking "Yujin's Space" in the cafe homepage will redirect user to `/cafe/yujin`.

We can model topic like:

```json
topics: [
  {
    topicId: "5e60734a-e538-4415-b9f4-4ac2ce7f687e",
    spaceId: "yujin",
    title: "Yujin's Fancam Collection Help",
    authorId: "20129625-c2bb-4113-b3e9-e76a3e41d78a",
	  content: "I need help finding more fancam of her"
  }
]
```

### Topic URL

A request to `/cafe/yujin` would filter every topics of `spaceId == "yujin"`. On a bigger scale though, maybe topics should be partitioned into their respective section. This results in multiple collections of topics grouped by their section.

The `topicId` and other IDs must be unique and rely on UUID. The first 8-characters will also be used for URL generation of the topic. In this case, the server should check for possible collision of the first 8-characters of the newly generated UUID.

More specifically, the URL of a topic will be a combination of the topic's title and its unique ID.

For example, `forumdomain.com/cafe/yujin/123e4567/yujin-s-fancam-collection-help`:

- `forumdomain.com` the domain name of the social forum website.
- `cafe` represent the cafe section of the website.
- `yujin` the section identifier `yujin`.
- `123e4567` is the first 8-characters of the `topicId`.
- `yujin-s-fancam-collection-help` is a slug string of the topic's title. It only includes (a-z, A-Z, 0-9, -).

Key points:

- The topic title is included in the URL for UX. This will let users know the rough subject of a forum post just by seeing the URL. This is also often called as _slug_.
- The purpose of including UUID in the URL is to prepare for potential URL change. The UUID is only the first 8-characters to avoid long URL.
  - User can edit the topic's title. When it is edited, a new URL with an altered slug will be produced.
  - The goal is to prevent dead links. When users open the old URL, they should be redirected to the new URL.
  - The server can achieve this by only using the UUID for identification. The server don't need to bother with whatever the title is.
  - It would retrieve the specific topic associated with this UUID, identified by the first 8-characters.
  - An actual dead link (not found) occurs when there are no matching IDs.
  - There are 4.2 billion unique combinations from the first 8-characters of UUID. Though, collision can still occur, so server should re-generate until the first 8-characters are different.

Any character should be allowed in the topic title. Enforce a minimum of 10 characters.

### Reply & Comment

Topic can be replied. Each reply itself can also be replied. We call a reply to a topic simply as **reply**, whereas a reply to another reply as **comment**.

Adding another reply just to respond to one of the topic's reply can make the post get out of context — comment is created for that. Comment is a direct response to a particular reply in the post.

The topic itself can't be commented — as it should be published as a normal reply instead. The comment contains a flat structure of responses. In other word, a comment can't be replied or be commented further. User can respond to a comment by writing another comment and tagging the user, but not creating another dedicated section. Comments on a reply is limited to the amount of 20.

Replies are stored in a separate collection from topic, modeled like:

```json
replies: [
  {
    replyId: "fd7a9a4a-8dca-4d36-96a2-06b55fb20055",
    topicId: "5e60734a-e538-4415-b9f4-4ac2ce7f687e",
    authorId: "b6718f06-cdea-4290-a8a8-c3f55b899b97",
    content: "I have a tons! Send me a letter.",
    comments: [
      commentId: "30b35baf-646d-4ce5-ae5d-0c69bb8488c6",
      authorId: "d1b9829f-a179-41b6-b3e3-100da391afbd",
      content: "Give me too please."
    ]
  },
]
```

Since comments are limited, for simplicity, they are embedded directly in the replies.

For example, the users:

```json
users: [
  {
    userId: "20129625-c2bb-4113-b3e9-e76a3e41d78a",
    username: "utokki_forever"
  },
  {
    userId: "b6718f06-cdea-4290-a8a8-c3f55b899b97",
    username: "yujinnie"
  },
  {
    userId: "d1b9829f-a179-41b6-b3e3-100da391afbd",
    username: "thinking_in_xiao"
  },
]
```

Overall, it will be displayed like:

```
"I need help finding more fancam of her" — "utokki_forever"

"I have a tons! Send me a letter." — "yujinnie"
 ├─ "Give me too please." — "thinking_in_xiao"
 └─ "Another comment..." — "some_username"

"another reply"  — "some_username"

"another reply"  — "some_username"
 ├─ "Another comment..." — "some_username"
 ├─ "Another comment..." — "some_username"
 └─ "Another comment..." — "some_username"
```

This mean loading a topic generate multiple queries:

1. Query topic by the short ID. This gives the full topic ID, title, author ID, and content.
2. Query replies by filtering the same topic ID. This produces N-amount of replies including its comments.
3. Collect every unique author ID (application-level) and query the users collection to obtain their profile information.

The backend query, summarize everything, and build the frontend model. The response to frontend would be: the topic model including title, content, and author information, a list of replies containing the reply content, author information, posted date, and a list of comments; each comment would also contain the content, author info, and posted date.

The topic post is rendered separately. The replies will be rendered in the order of the posted date. Each reply add a comment section if the corresponding reply has any comments.
