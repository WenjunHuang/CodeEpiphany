create table challenge
(
    id          integer not null
        constraint pk
            primary key autoincrement,
    title       TEXT    not null,
    slug        TEXT    not null,
    dojo        TEXT    not null,
    dojoId      TEXT    not null,
    difficulty  TEXT    not null,
    description TEXT    not null,
    tags        TEXT,
    remark      TEXT
);

create unique index challenge_dojoId_dojo_uindex
    on challenge (dojoId, dojo);

create table challenge_language
(
    challengeId     integer
        constraint challenge_language_challenge_id_fk
            references challenge
            on update set null on delete set null,
    language        TEXT            not null,
    codeTemplate    TEXT            not null,
    id              integer         not null
        constraint challenge_language_pk
            primary key autoincrement,
    languageVersion TEXT default '' not null
);

create unique index challenge_language_challengeId_language_uindex
    on challenge_language (challengeId, language);

create table hackerrank_challenge
(
    id          integer not null
        constraint hackerrank_challenge_pk
            primary key
        constraint hackerrank_challenge_challenge_id_fk
            references challenge,
    contestSlug TEXT    not null,
    contest     TEXT    not null
);

create table hackerrank_challenge_language
(
    id           integer not null
        constraint hackerrank_challenge_language_pk
            primary key
        constraint hackerrank_challenge_language_challenge_language_id_fk
            references challenge_language,
    codeHeader   TEXT    not null,
    codeTemplate TEXT    not null,
    codeTail     TEXT    not null
);

create table solution
(
    id                  integer           not null
        constraint solution_pk
            primary key autoincrement,
    challengeId         integer           not null
        constraint solution_challenge_id_fk
            references challenge,
    challengeLanguageId integer           not null
        constraint solution_challenge_language_id_fk
            references challenge_language,
    createDateTime      INTEGER           not null,
    tags                TEXT,
    remark              TEXT,
    title               TEXT              not null,
    isDefault           INTEGER default 0 not null
);

create unique index solution_challengeLanguageId_title_uindex
    on solution (challengeLanguageId, title);

create table solution_submission
(
    id               INTEGER not null
        constraint solution_submission_pk
            primary key autoincrement,
    dojoSubmissionId TEXT,
    submitDateTime   integer not null,
    localCode        TEXT    not null,
    submitCode       TEXT    not null,
    state            TEXT    not null,
    solutionId       integer not null
        constraint solution_submission_solution_id_fk
            references solution
);

create table hackerrank_solution_submission_result
(
    id             integer not null
        constraint hackerrank_solution_submission_result_pk
            primary key autoincrement,
    state          TEXT    not null,
    message        TEXT,
    num            integer not null,
    stdIn          TEXT,
    expectedOutput TEXT,
    submissionId   integer not null
        constraint hackerrank_solution_submission_result_solution_submission_id_fk
            references solution_submission
);
