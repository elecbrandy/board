-- 1. 사용자 (Users) - 비밀번호는 '1234'로 통일
-- 관리자 1명, 일반 유저 4명 (총 5명)
INSERT INTO users (email, password, username, role) VALUES ('admin@example.com', '1234', '관리자', 'ADMIN');
INSERT INTO users (email, password, username, role) VALUES ('user1@spring.com', '1234', '스프링고수', 'USER');
INSERT INTO users (email, password, username, role) VALUES ('user2@java.com', '1234', 'JPA초보', 'USER');
INSERT INTO users (email, password, username, role) VALUES ('user3@coding.com', '1234', '코딩하는호랑이', 'USER');
INSERT INTO users (email, password, username, role) VALUES ('user4@dev.com', '1234', '새벽코딩', 'USER');

-- 2. 카테고리 (Categories)
-- 이미 있는 것 외에 추가
INSERT INTO categories (name, description) VALUES ('공지사항', '서비스 필독 사항입니다.');
INSERT INTO categories (name, description) VALUES ('자유게시판', '자유롭게 수다 떠는 공간');
INSERT INTO categories (name, description) VALUES ('질문게시판', '궁금한 점을 물어보세요');
INSERT INTO categories (name, description) VALUES ('정보공유', '유용한 개발 팁을 공유합니다');
INSERT INTO categories (name, description) VALUES ('장터', '개발 서적 및 장비 거래');

-- 3. 게시글 (Posts)
-- 3-1. 공지사항 (관리자 작성)
INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (1, 1, '커뮤니티 오픈 공지', '반갑습니다. 클린한 커뮤니티가 됩시다.', 100);

INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (1, 1, '서버 점검 안내', '주말 간 서버 점검이 있을 예정입니다.', 55);

-- 3-2. 자유게시판 (다양한 유저들의 수다)
INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (2, 2, '오늘 점심 뭐 먹지?', '개발하다 보니 배가 너무 고프네요. 추천 좀.', 12);

INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (2, 3, '스프링 부트 너무 재밌네요', 'JPA 공부중인데 신기합니다. 영속성 컨텍스트 짱!', 45);

INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (2, 4, '퇴근하고 싶다...', '야근 중입니다. 살려주세요.', 5);

INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (2, 2, '개발자는 맥북 필수인가요?', '윈도우만 쓰다가 넘어가려는데 고민되네요.', 88);

-- 3-3. 질문게시판 (답변이 달릴 글들)
INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (3, 3, 'JPA N+1 문제 어떻게 해결하나요?', 'fetch join을 쓰라는데 감이 안 잡혀요 ㅠㅠ', 120);

INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (3, 4, '배포는 AWS 어디에 하나요?', 'EC2? S3? 초보라 어렵습니다.', 30);

-- 3-4. 정보공유 (인기 글 후보)
INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (4, 2, '인텔리제이 단축키 모음.zip', '이것만 알면 코딩 속도 2배!', 500);

INSERT INTO posts (category_id, user_id, title, content, view_count)
VALUES (4, 2, '주니어 개발자 면접 후기', '탈탈 털렸지만 공유합니다...', 300);


-- 4. 댓글 (Comments)
-- JPA 질문글(ID:7)에 대한 댓글들
INSERT INTO comments (post_id, user_id, content) VALUES (7, 2, '저도 궁금해요!');
INSERT INTO comments (post_id, user_id, content) VALUES (7, 1, '연관된 엔티티를 한방 쿼리로 가져오는 기능입니다. 구글링 해보세요!'); -- 고수 등장

-- 자유게시판 글(ID:3)에 대한 댓글
INSERT INTO comments (post_id, user_id, content) VALUES (3, 3, '국밥이 최고죠.');
INSERT INTO comments (post_id, user_id, content) VALUES (3, 4, '저는 햄버거 먹었습니다.');

-- 정보공유 글(ID:9)에 대한 감사 댓글
INSERT INTO comments (post_id, user_id, content) VALUES (9, 3, '오 꿀팁 감사합니다. 스크랩해갈게요.');
INSERT INTO comments (post_id, user_id, content) VALUES (9, 4, '와드 박고 갑니다.');
INSERT INTO comments (post_id, user_id, content) VALUES (9, 5, 'Ctrl+W가 진짜 편하더라고요.');


-- 5. 좋아요 (Post Likes)
-- 인기글(ID:9 - 단축키 모음)에 좋아요 몰아주기
INSERT INTO post_likes (user_id, post_id) VALUES (1, 9);
INSERT INTO post_likes (user_id, post_id) VALUES (3, 9);
INSERT INTO post_likes (user_id, post_id) VALUES (4, 9);
INSERT INTO post_likes (user_id, post_id) VALUES (5, 9);

-- 공감가는 글(ID:5 - 퇴근하고 싶다)에 좋아요
INSERT INTO post_likes (user_id, post_id) VALUES (2, 5);
INSERT INTO post_likes (user_id, post_id) VALUES (3, 5);


-- 6. 즐겨찾기/스크랩 (Post Favorites)
-- 유용한 정보글(ID:9, 10) 스크랩
INSERT INTO post_favorites (user_id, post_id) VALUES (2, 7); -- JPA 질문글 스크랩
INSERT INTO post_favorites (user_id, post_id) VALUES (3, 9); -- 단축키 글 스크랩
INSERT INTO post_favorites (user_id, post_id) VALUES (4, 9);


-- 7. 팔로우 (Follows)
-- 2번 유저(스프링고수)를 3, 4번이 팔로우
INSERT INTO follows (follower_id, following_id) VALUES (3, 2);
INSERT INTO follows (follower_id, following_id) VALUES (4, 2);

-- 서로 팔로우 (맞팔)
INSERT INTO follows (follower_id, following_id) VALUES (3, 4);
INSERT INTO follows (follower_id, following_id) VALUES (4, 3);