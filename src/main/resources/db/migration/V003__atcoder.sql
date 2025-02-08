CREATE TABLE "atcoder_challenge"
(
    id           integer not null
        constraint atcoder_challenge_pk
            primary key
        constraint atcoder_challenge_challenge_id_fk
            references challenge,
    difficulty   integer,
    contestId    TEXT    not null,
    problemIndex TEXT    not null,
    title        TEXT    not null
);
CREATE TABLE "atcoder_contests"
(
    contestId        TEXT    not null,
    startEpochSecond integer,
    durationSecond   integer,
    title            TEXT    not null,
    rateChange       TEXT,
    id               integer not null
        constraint atcoder_contests_pk
            primary key
);
CREATE VIRTUAL TABLE atcoder_contests_fts using fts5
(
    id UNINDEXED,
    contestId,
    startEpochSecond UNINDEXED,
    durationSecond UNINDEXED,
    title,
    rateChange UNINDEXED,
    tokenize="trigram",
    content='atcoder_contests',
    content_rowid='id'
);
CREATE TABLE atcoder_problems
(
    id                   integer not null
        constraint atcoder_problems_pk
            primary key,
    difficulty           INTEGER,
    problemId            TEXT    not null,
    contestId            TEXT    not null,
    contestTitle        TEXT,
    problemIndex         TEXT    not null,
    name                 TEXT    not null,
    title                TEXT    not null,
    shortestSubmissionId INTEGER,
    shortestContestId    TEXT,
    shortestUserId       TEXT,
    fastestSubmissionId  INTEGER,
    fastestContestId     TEXT,
    fastestUserId        TEXT,
    firstSubmissionId    INTEGER,
    firstContestId       TEXT,
    firstUserId          TEXT,
    solverCount          integer
);
CREATE VIRTUAL TABLE atcoder_problems_fts using fts5
(
    id UNINDEXED,
    difficulty,
    problemId,
    contestId,
    contestTitle,
    problemIndex,
    name,
    title,
    shortestSubmissionId UNINDEXED,
    shortestContestId UNINDEXED,
    shortestUserId UNINDEXED,
    fastestSubmissionId UNINDEXED,
    fastestContestId UNINDEXED,
    fastestUserId UNINDEXED,
    firstSubmissionId UNINDEXED,
    firstContestId UNINDEXED,
    firstUserId UNINDEXED,
    solverCount UNINDEXED,
    tokenize='trigram',
    content='atcoder_problems',
    content_rowid='id'
);

CREATE TRIGGER atcoder_problems_ad
    after delete
    on atcoder_problems
begin
    insert into atcoder_problems_fts(atcoder_problems_fts, rowid, id, difficulty, problemId, contestId,contestTitle, problemIndex,
                                     name, title,
                                     shortestSubmissionId, shortestContestId, shortestUserId, fastestSubmissionId,
                                     fastestUserId, firstSubmissionId, firstContestId, firstUserId, solverCount)
    values ('delete', old.id, old.id, old.difficulty, old.problemId, old.contestId,old.contestTitle, old.problemIndex, old.name,
            old.title,
            old.shortestSubmissionId, old.shortestContestId, old.shortestUserId, old.fastestSubmissionId,
            old.fastestUserId, old.firstSubmissionId, old.firstContestId, old.firstUserId, old.solverCount);
end;

CREATE TRIGGER atcoder_problems_ai
    after insert
    on atcoder_problems
begin
    insert into atcoder_problems_fts(rowid, id, difficulty, problemId, contestId,contestTitle, problemIndex, name, title,
                                     shortestSubmissionId,
                                     shortestContestId, shortestUserId, fastestSubmissionId, fastestUserId,
                                     firstSubmissionId, firstContestId, firstUserId, solverCount)
    values (new.id, new.id, new.difficulty, new.problemId, new.contestId,new.contestTitle, new.problemIndex, new.name, new.title,
            new.shortestSubmissionId, new.shortestContestId, new.shortestUserId, new.fastestSubmissionId,
            new.fastestUserId, new.firstSubmissionId, new.firstContestId, new.firstUserId, new.solverCount);
end;

CREATE TRIGGER atcoder_problems_au
    after update
    on atcoder_problems
begin
    insert into atcoder_problems_fts(atcoder_problems_fts, rowid, id, difficulty, problemId, contestId,contestTitle, problemIndex,
                                     name, title,
                                     shortestSubmissionId, shortestContestId, shortestUserId, fastestSubmissionId,
                                     fastestUserId, firstSubmissionId, firstContestId, firstUserId, solverCount)
    values ('delete', old.id, old.id, old.difficulty, old.problemId, old.contestId, old.contestTitle,old.problemIndex, old.name,
            old.title,
            old.shortestSubmissionId, old.shortestContestId, old.shortestUserId, old.fastestSubmissionId,
            old.fastestUserId, old.firstSubmissionId, old.firstContestId, old.firstUserId, old.solverCount);
    insert into atcoder_problems_fts(rowid, id, difficulty, problemId, contestId,contestTitle, problemIndex, name, title,
                                     shortestSubmissionId,
                                     shortestContestId, shortestUserId, fastestSubmissionId, fastestUserId,
                                     firstSubmissionId, firstContestId, firstUserId, solverCount)
    values (new.id, new.id, new.difficulty, new.problemId, new.contestId,new.contestTitle, new.problemIndex, new.name, new.title,
            new.shortestSubmissionId, new.shortestContestId, new.shortestUserId, new.fastestSubmissionId,
            new.fastestUserId, new.firstSubmissionId, new.firstContestId, new.firstUserId, new.solverCount);
end;

