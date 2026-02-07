# 기능별 요구사항 명세서

## 프로젝트 설정

- [x] Spring Boot 프로젝트 초기 설정
- [x] 필요한 의존성 추가 (Spring Web, JPA, PostgreSQL, Lombok 등)
- [x] application.yml 설정 (DB 연결, JPA 설정)
- [x] 공통 응답 DTO 구조 생성 (`ApiResponse<T>`)

## 🗄️ 데이터베이스 & 엔티티

### 엔티티 클래스 생성
- [x] User 엔티티
- [x] Category 엔티티
- [x] CategoryFavorite 엔티티
- [x] Post 엔티티
- [x] Comment 엔티티
- [x] PostLike 엔티티
- [x] PostFavorite 엔티티
- [x] Follow 엔티티

### 리포지토리 생성
- [x] UserRepository
- [x] CategoryRepository
- [x] CategoryFavoriteRepository
- [x] PostRepository
- [x] CommentRepository
- [x] PostLikeRepository
- [x] PostFavoriteRepository
- [x] FollowRepository



## 👥 Users API

### Controller
- [ ] UserController 생성

### Endpoints
- [ ] `POST /api/users` - 사용자 등록
- [ ] `GET /api/users` - 사용자 목록 조회 (페이지네이션)
- [ ] `GET /api/users/{id}` - 사용자 상세 조회
- [ ] `PUT /api/users/{id}` - 사용자 정보 수정
- [ ] `DELETE /api/users/{id}` - 사용자 삭제

### Service & DTO
- [ ] UserService 생성
- [ ] UserRequestDto 생성
- [ ] UserResponseDto 생성



## 📁 Categories API

### Controller
- [ ] CategoryController 생성

### Endpoints
- [ ] `POST /api/categories` - 카테고리 생성
- [ ] `GET /api/categories` - 카테고리 목록 조회
- [ ] `GET /api/categories/{id}` - 카테고리 상세 조회
- [ ] `PUT /api/categories/{id}` - 카테고리 수정
- [ ] `DELETE /api/categories/{id}` - 카테고리 삭제

### Service & DTO
- [ ] CategoryService 생성
- [ ] CategoryRequestDto 생성
- [ ] CategoryResponseDto 생성



## ⭐ Category Favorites API

### Controller
- [ ] CategoryFavoriteController 생성

### Endpoints
- [ ] `POST /api/users/{userId}/category-favorites` - 카테고리 즐겨찾기 추가
- [ ] `GET /api/users/{userId}/category-favorites` - 카테고리 즐겨찾기 목록 조회
- [ ] `DELETE /api/users/{userId}/category-favorites/{categoryId}` - 카테고리 즐겨찾기 삭제

### Service & DTO
- [ ] CategoryFavoriteService 생성
- [ ] CategoryFavoriteRequestDto 생성
- [ ] CategoryFavoriteResponseDto 생성



## 📝 Posts API

### Controller
- [ ] PostController 생성

### Endpoints
- [ ] `POST /api/posts` - 게시글 작성
- [ ] `GET /api/posts` - 게시글 목록 조회 (페이지네이션, 카테고리/사용자 필터링)
- [ ] `GET /api/posts/{id}` - 게시글 상세 조회 (조회수 증가)
- [ ] `PUT /api/posts/{id}` - 게시글 수정
- [ ] `DELETE /api/posts/{id}` - 게시글 삭제

### Service & DTO
- [ ] PostService 생성
- [ ] PostRequestDto 생성
- [ ] PostResponseDto 생성

### 추가 기능
- [ ] 조회수 증가 로직 구현



## 💬 Comments API

### Controller
- [ ] CommentController 생성

### Endpoints
- [ ] `POST /api/posts/{postId}/comments` - 댓글 작성
- [ ] `GET /api/posts/{postId}/comments` - 게시글의 댓글 목록 조회 (페이지네이션)
- [ ] `PUT /api/posts/{postId}/comments/{id}` - 댓글 수정
- [ ] `DELETE /api/posts/{postId}/comments/{id}` - 댓글 삭제

### Service & DTO
- [ ] CommentService 생성
- [ ] CommentRequestDto 생성
- [ ] CommentResponseDto 생성



## 👍 Likes API

### Controller
- [ ] PostLikeController 생성

### Endpoints
- [ ] `POST /api/posts/{postId}/likes` - 게시글 좋아요
- [ ] `GET /api/posts/{postId}/likes` - 게시글 좋아요 목록 조회
- [ ] `DELETE /api/posts/{postId}/likes/{userId}` - 게시글 좋아요 취소

### Service & DTO
- [ ] PostLikeService 생성
- [ ] PostLikeRequestDto 생성
- [ ] PostLikeResponseDto 생성

### 추가 기능
- [ ] 중복 좋아요 방지 로직 (UNIQUE 제약조건 처리)



## 🔖 Post Favorites API

### Controller
- [ ] PostFavoriteController 생성

### Endpoints
- [ ] `POST /api/users/{userId}/post-favorites` - 게시글 즐겨찾기 추가
- [ ] `GET /api/users/{userId}/post-favorites` - 게시글 즐겨찾기 목록 조회 (페이지네이션)
- [ ] `DELETE /api/users/{userId}/post-favorites/{postId}` - 게시글 즐겨찾기 삭제

### Service & DTO
- [ ] PostFavoriteService 생성
- [ ] PostFavoriteRequestDto 생성
- [ ] PostFavoriteResponseDto 생성

### 추가 기능
- [ ] 중복 즐겨찾기 방지 로직



## 🤝 Follows API

### Controller
- [ ] FollowController 생성

### Endpoints
- [ ] `POST /api/users/{followerId}/follows` - 사용자 팔로우
- [ ] `GET /api/users/{followerId}/follows` - 팔로잉 목록 조회
- [ ] `DELETE /api/users/{followerId}/follows/{followingId}` - 팔로우 취소
- [ ] `GET /api/users/{userId}/followers` - 팔로워 목록 조회

### Service & DTO
- [ ] FollowService 생성
- [ ] FollowRequestDto 생성
- [ ] FollowResponseDto 생성

### 추가 기능
- [ ] 자기 자신 팔로우 방지 로직
- [ ] 중복 팔로우 방지 로직



## 🛠️ 공통 기능

### 예외 처리
- [ ] GlobalExceptionHandler 생성
- [ ] 커스텀 예외 클래스 생성 (ResourceNotFoundException, DuplicateException 등)
- [ ] 공통 에러 응답 포맷 정의

### 유효성 검증
- [ ] DTO에 Bean Validation 어노테이션 추가 (@NotNull, @Size 등)
- [ ] 유효성 검증 실패 시 응답 처리

### 페이지네이션
- [ ] Pageable 파라미터 처리
- [ ] 페이지네이션 응답 DTO 표준화

### 로깅
- [ ] 요청/응답 로깅 설정
- [ ] 에러 로깅 설정



## ✅ 테스트

### 단위 테스트
- [ ] UserService 테스트
- [ ] CategoryService 테스트
- [ ] PostService 테스트
- [ ] CommentService 테스트
- [ ] PostLikeService 테스트
- [ ] PostFavoriteService 테스트
- [ ] CategoryFavoriteService 테스트
- [ ] FollowService 테스트

### 통합 테스트
- [ ] UserController 통합 테스트
- [ ] CategoryController 통합 테스트
- [ ] PostController 통합 테스트
- [ ] CommentController 통합 테스트
- [ ] PostLikeController 통합 테스트
- [ ] PostFavoriteController 통합 테스트
- [ ] CategoryFavoriteController 통합 테스트
- [ ] FollowController 통합 테스트

### API 테스트
- [ ] Postman/Swagger로 전체 API 엔드포인트 테스트
- [ ] 엣지 케이스 테스트 (존재하지 않는 리소스, 중복 요청 등)



## 📚 문서화

- [ ] Swagger/OpenAPI 설정
- [ ] API 문서 자동 생성 확인
- [ ] README.md 작성 (프로젝트 설명, 실행 방법)
- [ ] ERD 다이어그램 작성



## 🚀 배포 준비

- [ ] application-prod.yml 설정
- [ ] 환경변수 설정 (DB 접속 정보 등)
- [ ] Docker 설정 (선택사항)
- [ ] CI/CD 파이프라인 설정 (선택사항)



## 📊 진행 상황

- **전체 작업**: 0 / 120+
- **완료율**: 0%

### 우선순위
1. **High**: 프로젝트 설정, 엔티티/리포지토리, Users/Categories/Posts API
2. **Medium**: Comments, Likes, Favorites API
3. **Low**: Follows API, 테스트, 문서화



## 💡 추가 고려사항

- [ ] 인증/인가 (Spring Security + JWT)
- [ ] 비밀번호 암호화 (BCryptPasswordEncoder)
- [ ] CORS 설정
- [ ] 파일 업로드 기능 (프로필 이미지, 게시글 첨부파일)
- [ ] 검색 기능 (게시글 제목/내용 검색)
- [ ] 정렬 기능 (최신순, 인기순, 조회수순)
- [ ] 알림 기능 (새 댓글, 좋아요, 팔로우 알림)
- [ ] 캐싱 (Redis)
- [ ] 성능 최적화 (N+1 쿼리 해결, 인덱스 설정)