DROP TABLE IF EXISTS follows CASCADE;
DROP TABLE IF EXISTS post_favorites CASCADE;
DROP TABLE IF EXISTS post_likes CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS category_favorites CASCADE;

CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMP      DEFAULT NOW(),
    updated_at   TIMESTAMP
);

CREATE TABLE categories (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(50)     NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at   TIMESTAMP       DEFAULT NOW(),
    updated_at   TIMESTAMP
);

CREATE TABLE category_favorites (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    category_id BIGINT          NOT NULL,
    created_at  TIMESTAMP       DEFAULT NOW(),
    updated_at  TIMESTAMP

    CONSTRAINT fk_cat_fav_user     FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_cat_fav_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT uk_cat_fav_user_cat UNIQUE (user_id, category_id)
);

CREATE TABLE posts (
    id          BIGSERIAL       PRIMARY KEY,
    category_id BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    content     TEXT            NOT NULL,
    view_count  BIGINT          DEFAULT 0,
    created_at  TIMESTAMP       DEFAULT NOW(),
    updated_at  TIMESTAMP,
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_posts_user     FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE comments (
    id          BIGSERIAL       PRIMARY KEY,
    post_id     BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    content     TEXT            NOT NULL,
    created_at   TIMESTAMP       DEFAULT NOW(),
    updated_at   TIMESTAMP,

    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE post_likes (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    post_id     BIGINT          NOT NULL,
    created_at   TIMESTAMP       DEFAULT NOW(),
    updated_at   TIMESTAMP,

    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_likes_user_post UNIQUE (user_id, post_id)
);

CREATE TABLE post_favorites (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    post_id     BIGINT          NOT NULL,
    created_at   TIMESTAMP       DEFAULT NOW(),
    updated_at   TIMESTAMP,

    CONSTRAINT fk_scraps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_scraps_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT uk_scraps_user_post UNIQUE (user_id, post_id)
);

CREATE TABLE follows (
    id           BIGSERIAL       PRIMARY KEY,
    follower_id  BIGINT          NOT NULL,
    following_id BIGINT          NOT NULL,
    created_at   TIMESTAMP       DEFAULT NOW(),
    updated_at   TIMESTAMP,

    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_follow_follower_following UNIQUE (follower_id, following_id)
);

-- dummy data
INSERT INTO follows (follower_id, following_id) VALUES (1, 2);
INSERT INTO categories (name, description) VALUES ('공지사항', '필독 사항입니다.');
INSERT INTO categories (name, description) VALUES ('자유게시판', '자유롭게 글을 남겨주세요.');
INSERT INTO categories (name, description) VALUES ('질문게시판', '궁금한 점을 물어보세요.');
INSERT INTO users (email, password, username, role) VALUES ('admin@example.com', '1234', '관리자', 'ADMIN');
INSERT INTO users (email, password, username, role) VALUES ('user1@example.com', '1234', '스프링고수', 'USER');


INSERT INTO posts (category_id, user_id, title, content)
VALUES (1, 1, '커뮤니티 오픈합니다!', '반갑습니다. 환영합니다.'); -- 관리자가 공지사항 작성

INSERT INTO posts (category_id, user_id, title, content)
VALUES (2, 2, '스프링 부트 너무 재밌네요', 'JPA 공부중인데 신기합니다.'); -- 유저1이 자유게시판 작성

INSERT INTO comments (post_id, user_id, content) VALUES (2, 1, '화이팅입니다!'); -- 관리자가 유저1 글에 댓글

INSERT INTO post_likes (user_id, post_id) VALUES (1, 2); -- 관리자가 유저1 글에 좋아요
INSERT INTO post_favorites (user_id, post_id) VALUES (1, 2); -- 관리자가 유저1 글 스크랩