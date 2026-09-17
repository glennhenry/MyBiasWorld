---
title: Radio
slug: radio
description: Radio docs
---

The radio is a small, active announcement system that appears on every page of the website in the form of marquee text or located within the right sidebar. It differs with the bulettin board, which is a static, permanent system intended for persistent announcements.

### Source

From a class like `RadioService` any component in the server can send a content to be displayed in the radio. There are few ways that lead to how a radio event can be generated:

1. Sent by administrators: There could be a pipeline to send broadcast for administrators. It could serve a purpose like patch notes or some important system announcement.
   - "Patch note 1.31: update cafe, new badges, fix bugs"
   - "We will be closing the site for a maintenance tonight"
   - "Come and play billiard after a new update"
2. Pre-written: It may include things like guide, trivia, or facts about the system or group. Contrary to the first type of source, it has a purpose of being a filler that stays as a fixed broadcast and repeatedly shown after some period.
   - "Kep1er officially debuted in 3 January 2022, since then they have accomplished ..."
   - "In order to post a topic or reply, you need to register first."
   - "MyBiasWorld started its development from June 2026."
3. [Activity](/docs/activity): Particular type of activity may be broadcasted through the radio according to their consumers.
