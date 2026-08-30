---
title: Attendance
slug: attendance
description: Attendance docs
---

When the user logs in, they attended that day. This would increase the login streak by 1. There would also be longest streak and total day attended.

We may not be place attendance on each user's profile, because it grows indefinitely and becomes a log rather than an identity. Instead, we create an attendance table of every users.

For example:

- `userId123`, `2026-8-19` -> represent an attendance done by `userId123` at 19 August 2026.

However, this little data can eventually balloon, and a good way to reduce is by making attendance per month. This is also intuitive because you would be updating attendance or querying attendance per-month anyway.

- `userId123`, `2026`, `08`, `[1, 2, 3, 4, 5, 6, 8, 9, 10, 11]` -> represent an attendance of `userId123` at August 2026 at day 1, 2, 3, and so on.

Alternatively, it can be further reduced to one per year or even a single attendance model for every users.
