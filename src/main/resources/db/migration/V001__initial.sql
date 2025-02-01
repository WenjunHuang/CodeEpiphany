create table challenge
(
    id          integer not null
        constraint pk
            primary key,
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
            primary key,
    languageVersion TEXT default '' not null
);

create unique index challenge_language_challengeId_language_uindex
    on challenge_language (challengeId, language);

create table codeforces_challenge
(
    id             INTEGER not null
        constraint codeforces_challenge_pk
            primary key
        constraint codeforces_challenge_challenge_id_fk
            references challenge,
    contestId      integer,
    "index"        TEXT    not null,
    problemsetName TEXT
);

create table codeforces_problemsets
(
    id                 INTEGER not null
        constraint codeforces_problemsets_pk
            primary key,
    contestId          INTEGER,
    solvedCount        INTEGER,
    lastUpdateDateTime INTEGER,
    rating             INTEGER,
    points             REAL,
    "index"            TEXT    not null,
    name               TEXT    not null,
    type               TEXT    not null,
    contestIdIndex     TEXT    not null,
    tags               TEXT,
    problemsetName     TEXT
);

create index codeforces_problemsets_contestId_index_index
    on codeforces_problemsets (contestId, "index");

create virtual table codeforces_problemsets_fts using fts5
(
    id UNINDEXED,
    contestId,
    solvedCount UNINDEXED,
    lastUpdateDateTime UNINDEXED,
    rating,
    points UNINDEXED,
    "index",
    name,
    type,
    contestIdIndex,
    tags,
    problemsetName,
    content='codeforces_problemsets',
    content_rowid='id'
);

CREATE TRIGGER codeforces_problemsets_ad
    after delete
    on codeforces_problemsets
begin
    insert into codeforces_problemsets_fts(codeforces_problemsets_fts,rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values ('delete',old.id, old.id, old.contestId, old.solvedCount, old.lastUpdateDateTime, old.rating, old.points, old."index", old.name, old.type, old.contestIdIndex, old.tags, old.problemsetName);
end;

CREATE TRIGGER codeforces_problemsets_ai
    after insert
    on codeforces_problemsets
begin
    insert into codeforces_problemsets_fts(rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values (new.id, new.id, new.contestId, new.solvedCount, new.lastUpdateDateTime, new.rating, new.points, new."index", new.name, new.type, new.contestIdIndex, new.tags, new.problemsetName);
end;

CREATE TRIGGER codeforces_problemsets_au
    after update
    on codeforces_problemsets
begin
    insert into codeforces_problemsets_fts(codeforces_problemsets_fts,rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values ('delete',old.id, old.id, old.contestId, old.solvedCount, old.lastUpdateDateTime, old.rating, old.points, old."index", old.name, old.type, old.contestIdIndex, old.tags, old.problemsetName);
    insert into codeforces_problemsets_fts(rowid, id,contestId,solvedCount,lastUpdateDateTime,rating,points,"index",name,type,contestIdIndex,tags,problemsetName)
    values (new.id, new.id, new.contestId, new.solvedCount, new.lastUpdateDateTime, new.rating, new.points, new."index", new.name, new.type, new.contestIdIndex, new.tags, new.problemsetName);
end;

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

create table leetcode_challenge
(
    frontendQuestionId TEXT    not null,
    testCase           TEXT,
    id                 INTEGER not null
        constraint leetcode_challenge_pk
            primary key
        constraint leetcode_challenge_challenge_id_fk
            references challenge
);

create table solution
(
    id             integer           not null
        constraint solution_pk
            primary key,
    challengeId    integer           not null
        constraint solution_challenge_id_fk
            references challenge,
    createDateTime INTEGER           not null,
    tags           TEXT,
    remark         TEXT,
    title          TEXT              not null,
    isDefault      INTEGER default 0 not null
);

create table solution_submission
(
    id                  INTEGER not null
        constraint solution_submission_pk
            primary key,
    dojoSubmissionId    TEXT,
    submitDateTime      integer not null,
    localCode           TEXT    not null,
    submitCode          TEXT    not null,
    result              TEXT    not null,
    solutionId          integer not null
        constraint solution_submission_solution_id_fk
            references solution,
    challengeLanguageId integer not null
        constraint solution_submission_challenge_language_id_fk
            references challenge_language,
    score               TEXT,
    message             TEXT,
    resultDateTime      integer
);

create table hackerrank_submission_case
(
    id                integer not null
        constraint hackerrank_submission_case_pk
            primary key,
    testcaseMessage   TEXT,
    num               integer not null,
    stdIn             TEXT,
    expectedOutput    TEXT,
    submissionId      integer not null
        constraint hackerrank_submission_case_solution_submission_id_fk
            references solution_submission,
    codecheckerSignal integer,
    codecheckerTime   REAL,
    testcaseStatus    integer
);

create table leetcode_submission
(
    id                INTEGER not null
        constraint leetcode_submission_pk
            primary key
        constraint leetcode_submission_solution_submission_id_fk
            references solution_submission,
    memory            integer,
    totalCorrect      integer,
    totalTestcases    integer,
    statusMemory      TEXT,
    runtimePercentile REAL,
    memoryPercentile  REAL,
    lastTestcase      TEXT,
    inputFormatted    TEXT,
    expectedOutput    TEXT,
    codeOutput        TEXT,
    stdOutput         TEXT,
    statusRuntime     TEXT
);


