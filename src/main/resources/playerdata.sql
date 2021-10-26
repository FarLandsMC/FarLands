CREATE TABLE IF NOT EXISTS playerdata (
    uuid BLOB PRIMARY KEY NOT NULL UNIQUE,
    username VARCHAR(16),
    discordID INT(8),
    lastLogin INT(8),
    nickname VARCHAR(64),
    lastIP VARCHAR(15),
    secondsPlayed INT(3),
    totalVotes INT(2),
    monthVotes INT(1),
    voteRewards INT(2),
    amountDonated INT(2),
    shops INT(1),
    flags INT(1),
    particles_type INT(1),
    particles_location INT(1),
    rank INT(1),
    lastLocation_world INT(1),
    lastLocation_x DOUBLE,
    lastLocation_y DOUBLE,
    lastLocation_z DOUBLE,
    lastLocation_yaw FLOAT,
    lastLocation_pitch FLOAT,
    currentMute_dateEnds INT(8),
    currentMute_reason VARCHAR(256),
    ignoredPlayers BLOB
);

CREATE TABLE IF NOT EXISTS punishments (
    uuid BLOB NOT NULL,
    punishmentType INT(1),
    dateIssued INT(8),
    message VARCHAR(256),
    FOREIGN KEY(uuid) REFERENCES playerdata(uuid)
);

CREATE TABLE IF NOT EXISTS homes (
    uuid BLOB NOT NULL,
    name VARCHAR(32),
    x DOUBLE,
    y DOUBLE,
    z DOUBLE,
    yaw FLOAT,
    pitch FLOAT,
    FOREIGN KEY(uuid) REFERENCES playerdata(uuid)
);

CREATE TABLE IF NOT EXISTS mail (
    uuid BLOB NOT NULL,
    sender VARCHAR(16),
    message VARCHAR(256),
    FOREIGN KEY(uuid) REFERENCES playerdata(uuid)
);

CREATE TABLE IF NOT EXISTS notes (
    uuid BLOB NOT NULL,
    dateTaken INT(8),
    sender VARCHAR(16),
    note VARCHAR(256),
    FOREIGN KEY(uuid) REFERENCES playerdata(uuid)
);