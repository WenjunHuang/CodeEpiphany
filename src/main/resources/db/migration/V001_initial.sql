create table challenge
(
    id           integer not null,
    title        TEXT    not null,
    slug         TEXT    not null,
    dojo         TEXT    not null,
    dojoId       TEXT    not null,
    difficulty   TEXT    not null,
    language     TEXT    not null,
    description  TEXT    not null,
    codeTemplate TEXT    not null,
    tags         TEXT,
    remark       TEXT
);

create table hackerrank_challenge
(
    challengeId  integer not null
        constraint hackerrank_challenge_challenge_id_fk
            references challenge (id),
    contestSlug  TEXT    not null,
    contest      TEXT    not null,
    codeHeader   TEXT    not null,
    codeTemplate TEXT    not null,
    codeTail     TEXT    not null
);

