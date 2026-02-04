# Board

## 개발 순서

- API 설계
  - 엔드포인트 설정
  - Request/Response JSON 구조 정하기
- Backend 개발
  - DB ERD 기반 Entity 작성
  - Repository 작성
  - DTO 작성
  - Service 작성
  - Controller 작성
- 기능 개발 흐름
  - 회원가입/로그인 → 카테고리 목록 조회 → 게시글 작성 및 목록 조회 → 좋아요/스크랩/팔로우 등

## 요구사항 명세

### Setting
- [x] **Docker & Makefile**
  - [x] `docker-compose.yml` 작성: PostgreSQL 15 컨테이너 정의 (Port: 5432, Volume 마운트 설정)
  - [x] `Makefile` 작성: 컨테이너 실행(up), 중지(down), 로그 확인(logs), DB 접속(psql) 명령어 등록
- [x] **애플리케이션 설정**
  - [x] `src/main/resources/application.yaml` 작성
  - [x] Datasource 연결 정보 설정
  - [x] JPA 설정: `ddl-auto: none`, SQL 로깅 활성화
  - [x] SQL 초기화 설정: `spring.sql.init.mode: always`

### DB
- [x] **ERD 설계 -> .sql 작성 (`src/main/resources/schema.sql`)**
  - [x] 초기화 구문(DROP TABLE) 작성
  - [x] `users` 테이블 생성 (id, email, password, nickname, role, created_at)
  - [x] `categories` 테이블 생성 (id, name, description)
  - [x] `posts` 테이블 생성 (id, category_id, user_id, title, content, view_count, dates)
  - [x] `comments` 테이블 생성 (id, post_id, user_id, content, created_at)
  - [x] `post_Favorites` 테이블 생성 (user_id, post_id, unique constraint)
  - [x] `post_scraps` 테이블 생성 (user_id, post_id, unique constraint)
- [x] 초기 데이터 구성 (`schema.sql` 하단)
  - [x] 기본 카테고리 데이터 삽입 (예: 자유게시판, 질문게시판)
  - [x] 테스트용 사용자 데이터 삽입

### Spring boot

#### User
- [x] **Entity 구현**
  - [x] `User` 클래스 생성 및 테이블 매핑
  - [x] `BaseTimeEntity` 적용 (생성일, 수정일 자동화)
- [ ] **Repository 구현**
  - [ ] `UserRepository` 인터페이스 생성 (`JpaRepository` 상속)
- [ ] **API 구현 (Controller/Service)**
  - [ ] 회원가입 API (POST /api/users)
  - [ ] 회원 조회 API (GET /api/users/{id})

#### Category
- [x] **Entity 구현**
  - [x] `Category` 클래스 생성
  - [x] `BaseTimeEntity` 적용 (생성일, 수정일 자동화)
- [ ] **Repository 구현**
  - [ ] `CategoryRepository` 인터페이스 생성
- [ ] **API 구현**
  - [ ] 카테고리 목록 조회 API (GET /api/categories)

#### Post
- [x] **Entity 구현**
  - [x] `Post` 클래스 생성
  - [x] 연관관계 매핑: `User`(N:1), `Category`(N:1)
  - [x] `BaseTimeEntity` 적용 (생성일, 수정일 자동화)
- [ ] **Repository 구현**
  - [ ] `PostRepository` 인터페이스 생성
- [ ] **API 구현**
  - [ ] 게시글 작성 API (POST /api/posts)
  - [ ] 게시글 단건 조회 API (GET /api/posts/{id})
  - [ ] 게시글 목록 조회 API (GET /api/posts?page=0&size=10)
  - [ ] 게시글 수정 API (PUT /api/posts/{id})
  - [ ] 게시글 삭제 API (DELETE /api/posts/{id})

#### Comment
- [ ] **Entity 구현**
  - [ ] `Comment` 클래스 생성
  - [ ] 연관관계 매핑: `Post`(N:1), `User`(N:1)
- [ ] **Repository 구현**
  - [ ] `CommentRepository` 인터페이스 생성
- [ ] **API 구현**
  - [ ] 댓글 작성 API (POST /api/posts/{postId}/comments)
  - [ ] 특정 게시글의 댓글 목록 조회 API (GET /api/posts/{postId}/comments)
  - [ ] 댓글 삭제 API (DELETE /api/comments/{commentId})

#### Post Favorite
- [ ] **Entity 구현**
  - [ ] `PostFavorite` 클래스 생성
  - [ ] `BaseTimeEntity` 적용 (생성일, 수정일 자동화)
- [ ] **Repository 구현**
  - [ ] `PostFavoriteRepository`, `PostScrapRepository` 생성
- [ ] **API 구현**
  - [ ] 좋아요 등록/취소 토글 API (POST /api/posts/{postId}/Favorite)
  - [ ] 스크랩 등록/취소 토글 API (POST /api/posts/{postId}/scrap)

#### Post Like
- [ ] **Entity 구현**
  - [ ] `PostLike` 클래스 생성
  - [ ] `BaseTimeEntity` 적용 (생성일, 수정일 자동화)
- [ ] **Repository 구현**
  - [ ] `PostLikeRepository`, `PostLikeRepository` 생성
- [ ] **API 구현**
  - [ ] 좋아요 등록/취소 토글 API (POST /api/posts/{postId}/Favorite)
  - [ ] 스크랩 등록/취소 토글 API (POST /api/posts/{postId}/scrap)

#### Category Favorite
- [ ] **Entity 구현**
  - [ ] `CategoryFavorite` 클래스 생성
  - [ ] `BaseTimeEntity` 적용 (생성일, 수정일 자동화)
- [ ] **Repository 구현**
  - [ ] `CategoryFavoriteRepository` 생성
- [ ] **API 구현**
  - [ ] 좋아요 등록/취소 토글 API (POST /api/posts/{postId}/Favorite)
  - [ ] 스크랩 등록/취소 토글 API (POST /api/posts/{postId}/scrap)

#### Test
- [ ] **인프라 연동 테스트**
  - [ ] `make up` 실행 시 DB 컨테이너 정상 구동 확인
  - [ ] 애플리케이션 실행 시 테이블 자동 생성 여부 확인
- [ ] **API 기능 테스트**
  - [ ] Postman 또는 cURL을 이용한 주요 API(CRUD) 요청 및 응답 검증
  - [ ] DB 데이터 적재 확인 (`make psql` 활용)
