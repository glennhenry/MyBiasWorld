---
title: Session
slug: session
description: Session docs
---

Bias Cafe is a website application.

The session model used in the website is as follow:

- A session is identified with a UUID.
- It has `userId` to which account does the session belongs to.
- It has `expiresAt` which is the timestamp when it will expire.
- A session will last for 365 days, and this can be refreshed every 30 days.
- User that register or login will create a new session, this is stored as cookie in local storage.
- The server will store every user sessions in the memory and in MongoDB as backup. The memory remain as the main operator.

Some routes require cookie check and some do not. There should be three models:

- NoAuthGuard: nothing related to auth is checked, every requests pass through
- OptionalAccount: this only check the cookie; used for most website routes to create greeting message like "Welcome back, Alice" or "Logged in as Alice"
- RequireAccount: this is for routes where account is required, such as posting a cafe post, editing own's profile, messaging other users.

Refresh behavior:

- Session can be refreshed unlimited times.
- Refresh is done when at least 30 days passed on expiresAt (e.g., <= 335 days)
- Server will also periodically clean token session, maybe every 1 day, if the token is no longer valid (more than 365 days)

The behavior:

- user register/login, a session is generated which will be valid for the next 365 days. the server also return a cookie which is saved in user local storage
- for each request to any routes, the cookie is sent (use path '/')
- if cookie not exist, this mean user hasn't logged in.
  - in this case, server returns "not logged in" text in the template
- if cookie exist
  - server check if that cookie is valid (i.e., exist in the store) and it hasn't expire
  - if cookie is valid
    - server returns "logged in" text in the template
    - user can do any auth required action (e.g., create new post)
    - if there is at least 30 days passed on expiresAt (e.g., <= 335 days), server will reset it back to 365 days.
  - if cookie is invalid (expired)
    - server returns "not logged in" text in the template
    - the session saved in the server will also be deleted, if exists

Other case:

- when user log out, the session will be deleted
- when user login again, maybe in another device, new session is created normally
- in other word, multiple session can exist for one userId

Also need persistence backup with mongodb.

- on server startup, load the persistented session data
- on new session created (each register/login) run mongodb insertone
- on session refresh run mongodb updateone
- on session deleted (user logout) run mongodb deleteone
- data model should be {sessionId: expiresAt}
- do a periodic cleanup which delete every expired session (expiresAt is more than now millis)

For example:

- user first time visit on website "not logged in" (OptionalAccount guard)
- user register an account and now see "logged in as ..." (session is created, inserted to mongodb, valid for 365 days)
- user open forum section, they still see "logged in as ..." (OptionalAccount guard)
- user never open the website again until the next 10 days, still see "logged in as ..." (OptionalAccount guard, remaining session 355 days)
- user never open the website again until the next 21 days, still see "logged in as ..." (OptionalAccount guard, remaining session 334 days, refreshed back to 365 days)
- user never open the website again until the next 365 days, cookie may still exist in the browser, but it's now invalid. so user have to login again.
