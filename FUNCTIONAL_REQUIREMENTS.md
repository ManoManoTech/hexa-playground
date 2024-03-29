## HeroesDesk: mission tracker for heroes :)

No time tracking, no endless workflow customizing, only missions that need to be done. Or not: let the heroes decide :)

There are the following user types:

- users: able to see all and create missions (title and description)
- heroes: as users plus ability to modify all in missions in their squads
- admin: define squads, used for missions, as well as heroes allowed to work on each squad

A mission is made of:

- squad: one among the squads defined by the admin
- title: mandatory, updatable, single line, 1 up to 255 chars
- description: optional, updatable, multi line, up to 1024 chars
- creator: mandatory, fixed at creation
- id: unique among all missions, fixed at creation, made of the squad key, a dash and a unique number, 2 to 73 chars
- assignees: can be empty, updatable. Always empty for done mission.
- a mission can be pending, in progress or done: default to pending
    - each state can be moved to any of the 2 others
    - if a mission without assigned is set to in progress, the hero doing the action is automatically added as assignee
    - when a mission is done, then all its assignees are removed

Multiple missions with same title or description can be created.

A squad has:
- a name, 1 up to 255 chars, unique among all squads, updatable, must be unique
- a key, unique among all squads, by default deduced from the name, 1 to 36 chars, must be unique
- can't be changed
- assignees: can be empty, updatable

User management should be linkable to a preexisting user directory.