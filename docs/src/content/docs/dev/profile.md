---
title: Profile
slug: profile
description: Profile docs
---

Account (`UserAccount` in code) contains system or operational information about a user such as `userId`, `username`, `email`, `hashedPassword`, `registeredAt`, etc.

`Profile`, on the other hand, is the domain data of users. It includes user's personal information, display name, avatar URL, account level, currencies, etc. It doesn't include a data that keeps growing such as guestbook, scrapbook, mails, etc.

### Credentials

The username is a unique ID that users use to login and identification. It is created on registration and can be changed later. It should only contain lowercase alphabets, numbers, and underscore (a-z, 0-9, \_). It may be shown in user's profile.

On the other hand, the display name will be displayed across the website. It doesn't have to be unique and may be changed often. The user will starts out with their username used as the display name.

A similar system can be found in YouTube and Discord: a user has unique username ID e.g., "@yujin_580" but display name could be "유진 fan 😊".

As for email, it isn't used for identification or login, but is used when user forgot their password (cannot be used without email system in the server).

### Avatar

A registered account gets a random avatar from the server. The server provides a set of default avatars. The avatars should be neutral, non-offensive, and memorable. It doesn't need to be detailed — a chibi graphics should do it.

Since it's cafe themed, the avatars could be foods:

- Coffee cup
- Tea cup
- Milk
- Cake
- Croissant
- Toast
- Cookie

The purpose of default avatar is to give the site a memorable icon. There shouldn't be too many avatars or them being too good — otherwise users won't customize their profile themselves.

Every avatar images goes to `assets/avatars/...`. When user upload avatar, it is saved in that directory. Default avatar is also saved there. On user's profile, the `avatarUrl` field will point to the link, for example `avatarUrl = avatars/smile.png`.

Uploading an avatar will generate a UUID for the avatar file name. For example, `avatarUrl = avatars/123e4567-e89b-12d3-a456-426614174000`. The user's profile field will also be updated accordingly.

### Profile View

The left side of profile will be user's avatar, display name, and badge view. User can edit the badge view, choosing which badges to show in the profile and cafe.

Profile information can be grouped:

- Overview (editable): Username, display name, country, birthday, site join date, last active, bio, and a special guestbook down below where other users can left comment. When editing this, guestbook will be hide.
- Fan Profile (editable): my bias, favorite song, favorite era, started stanning at, what you like about them text field, etc.
- Status: user's level, coins, attendance, ranking, badges, achievements, and other cosmetics.
- Social (remove friend, unblock user button): friends, friend request, blocked users
- Activity: user's activity from posts, comments, likes, quests completed, badges earned, achievements completed, etc. This is filterable.
- Scrapbook (remove bookmark button): bookmark of contents.
- Statistics: number of posts, comments, likes, completed polls, day streak, number of completed quests. every numbers is listed here.

As for user's setting, it will be on another page.

- Settings: user setting of the site, change username, change email, change preferences, etc.


### Profile Stats & Summary

The related data of one's profile is separated around collections. For example, user's bio is profile, their posted topics live in an independent collection. We do not want to rely on querying multiple collections just to get simple data like count, instead we should make a summarized view or stats tracker of user's data.

This can be placed in profile and separated per domain, such as:

- `GeneralStats`: time active, and many random facts;
- `CafeStats`: num posts, num comments, num votes, polls casted;
- `AttendanceStats`: total attendance, current streak, longest streak;

A non-personal stats like user's ranking on the cafe may not be placed here, but instead in `ServerObjects`. This mean few updates to multiple collections must be made, such as:

- user posted in cafe -> insert topic -> update cafe stats -> update ranking in `ServerObjects`.
- user logged in -> update attendance table -> update personal attendance stats -> update ranking in `ServerObjects`.

