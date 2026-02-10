# Study

erd 들고

Enitity 만들기


서비스 메소드는 결국 뭘 바노한해야할까? Response 클래스 뭔가 예쁘게 담아서?
그러면 코드가 깔끔해질거같음.
그런데 서비스의 역할은 비즈니스로직이지 응답 포맷이 아님...
어처피 api 와 소통하는 첫번째 창구는 컨트롤러. 거기서 포장하고 포장을 뜨ㄷ는다.
서비스의 반환 타입이 ApiREsponse 로 통일되는게 일관성있다고 생각했으나,
- Controller: "나는 요청을 받고 응답(ApiResponse)을 포장해."
- Service: "나는 비즈니스 로직을 돌리고 결과 데이터(DTO)만 줘. 나는 웹인지 앱인지 몰라."
- Repository: "나는 DB랑만 대화해."
그러니까 결국 서비스는 요청 DTO를 받고, 그걸 엔티티로 바꿔서, 사용자가 원하는 값을 JPA로 엔티티를 마저 채우고, 그걸
적절한 응답용 DTO에 담아서 리턴한다.

유저가 회원가입 하는 과정을 잘 살펴보자
유저의 회원가입 요청 request는 이렇게 올 것

``` json
{
  "email": "example@example.com",
  "password": "1234",
  "username": "string"
}
```

그럼 우선 이건 post 요청이니까, 디스패처 서블릿에 의해 적절한 컨트롤러를 찾아 들어올것.
그것이 바로 UserController

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ApiResponse<UserJoinResponse> join(@Valid @RequestBody UserJoinRequest request) {
        // Service 호출
        UserJoinResponse userResponse = userService.join(request);

        // Response 포장해 return
        return ApiResponse.success("회원가입 성공", userResponse);
    }


}
```

중에서도 저 UserController.join 메소드가 받아서 처리할거야.
UserController.join은 파라미터로 `@Valid @RequestBody UserJoinRequest request` 를 받는다.
1. 우선 이건 유저의 요청 즉 json으로 들어온걸 spring 으로 처리하기 쉽게 만든 DTO 클래스이다. 아래처럼 생김
2. @Valid는 DTO 안에서 사용한 여러가지 제약들 등 발리데이션을 적용하기위해 쓴것이고,(이렇게 밖에서도 써줘야함)
3. 그리고 @RequestBody는 가장 먼저 작동함 웹이나 앱에서받은 json 데이터를 자바가 이해할수있는 DTO(우리가 아까 만든)를 
4. 클래스로 매핑해줌. 스프링부트 Jackson 라이브러리로. 그럼 json의 key와 DTO의 field 를 매칭시켜서 값을 채워 넣음.
5. 이 과정을 역직렬화라고함. 그럼 이제 객체가 생기면 @Valid가 필드를 스캔하면서 검사.

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJoinRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 4)
    private String password;

    @NotBlank
    @Size(min = 5, max = 50)
    private String username;

    // DTO -> Entity
    public User toEntity() {
        return User.builder()
                .email(email)
                .password(password)
                .username(username)
                .role("USER")
                .build();
    }
}
```

즉 이 DTO는 User가 join 요청을 보냈을떄 컨트롤러-서비스 지역에서 쓰이는 데이터 주머니임.
보면 유저가 requset를 보낼때 보내는 것들이 다 있음. email, password, username.
그러면 이 dto에 담아와서 컨트롤러에서 toentity로 해서 엔티



_____


**매우 날카로운 지적입니다!** "검증 로직 때문에 DB를 갔다 오는 것(Network I/O)이 성능에 영향을 주지 않을까?"라는 고민은 백엔드 개발자로서 아주 중요한 시각입니다.

결론부터 말씀드리면, **"DB에 다녀오는 것 자체는 필수불가결하지만, `findAll()` 방식은 최악이고 `existsBy...` 방식은 매우 효율적"**입니다.

왜 그런지, 그리고 실무에서는 이 문제를 어떻게 해결하는지 3단계로 나누어 설명해 드릴게요.

---

### 1. `findAll()` vs `existsBy...`의 성능 차이 (천지차이)

작성자님이 보신 코드(`findAll().stream()...`)와 제가 추천드린 코드(`existsBy...`)는 둘 다 DB를 다녀오지만, **DB 내부에서 하는 일**은 완전히 다릅니다.

#### ❌ `findAll().stream()`: 최악의 비효율 (Full Scan)

* **동작:** DB에 있는 **모든** 유저 데이터를 다 퍼옵니다. (SELECT * FROM users)
* **메모리:** 유저가 100만 명이면, 자바 메모리(Heap)에 100만 개의 객체를 다 올립니다.
* **CPU:** 그 100만 개를 반복문(stream)으로 하나씩 비교합니다.
* **비유:** 도서관에 있는 책이 100만 권인데, 특정 책이 있는지 확인하려고 **100만 권을 다 트럭에 실어서 집으로 가져온 뒤** 하나씩 찾는 격입니다. **서버가 뻗습니다.**

#### ✅ `existsByEmail(...)`: 최적화된 조회 (Index Scan)

* **동작:** DB에 "이 이메일 가진 사람 있어?"라고 딱 하나만 물어봅니다.
* 실제 쿼리: `SELECT 1 FROM users WHERE email = '...' LIMIT 1`


* **인덱스(Index):** 보통 `email` 컬럼에는 인덱스(색인)가 걸려 있습니다. DB는 데이터를 다 뒤지는 게 아니라, 색인만 훑어서 **0.001초** 만에 결과를 줍니다.
* **비유:** 도서관 컴퓨터에서 **검색**만 딱 해보는 겁니다. 책이 100만 권이든 1000만 권이든 검색 속도는 거의 차이가 없습니다.

---

### 2. "그래도 DB를 갔다 오는 건 맞잖아요?" (네트워크 비용)

맞습니다. `existsBy`를 써도 **네트워크 통신 비용**은 발생합니다. 하지만 이는 **"필요한 비용"**입니다.

1. **데이터의 무결성(Integrity):**
* "중복 가입 방지"는 서버 메모리가 아니라, **영구 저장소(DB)가 기준**이어야 합니다.
* 서버가 여러 대(Scale-out)라면? A서버 메모리에는 정보가 없어도, B서버를 통해 가입한 유저가 DB에는 있을 수 있습니다.


2. **비용 대비 효과:**
* `existsBy` 쿼리는 매우 가볍습니다. 이 정도의 네트워크 비용은 데이터 무결성을 지키기 위해 충분히 지불할 만한 가치가 있습니다.



---

### 3. 실무에서의 2중 안전장치 (동시성 이슈 해결)

사실 `existsBy...` 만으로는 완벽하지 않습니다. 아주 찰나의 순간에 문제가 생길 수 있습니다.

> **상황:** 유저 A와 B가 동시에 같은 이메일(`test@gmail.com`)로 가입 버튼을 눌렀습니다.

1. A 요청: `existsByEmail` -> 없음 (통과)
2. B 요청: `existsByEmail` -> 없음 (통과) - **(A가 아직 저장하기 전이라서)**
3. A 저장: `save()` -> 성공
4. B 저장: `save()` -> **중복 저장됨 (망함)**

이런 **경쟁 상태(Race Condition)**를 막기 위해 실무에서는 **DB 레벨의 제약조건(Unique Constraint)**을 최후의 보루로 사용합니다.

```sql
-- DB 스키마에 유니크 제약조건을 걸어둡니다.
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

```

이렇게 하면, 위 상황에서 B가 `save()`를 할 때 DB가 **"잠깐! 이거 중복이야!"** 하고 에러(`DataIntegrityViolationException`)를 뱉어냅니다.

**정리하자면:**

1. **Controller/Service:** `existsBy...`로 빠르게 1차 검증 (사용자에게 친절한 에러 메시지 제공용)
2. **Database:** `Unique Index`로 2차 검증 (절대 뚫리지 않는 최후의 방패)

---

### 결론 및 제안

질문하신 내용에 대한 답변은 **"findAll은 매우 비효율적이지만, existsBy는 인덱스를 타기 때문에 효율적이며 필수적인 과정이다"**입니다.

따라서 코드를 이렇게 수정하시면 완벽합니다.

**1. Repository에 메서드 추가**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // DB가 최적화된 쿼리를 날려줍니다.
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

```

**2. Service 수정**

```java
private void validateDuplicateUser(UserJoinRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalStateException("이미 존재하는 이메일입니다.");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new IllegalStateException("이미 존재하는 닉네임입니다.");
    }
}

```

이렇게 하면 성능 걱정 없이 안전하게 중복 체크를 하실 수 있습니다.


## JWT 토큰 도입

로그인까지 완료했으니까 JWT 토큰 도입하자.

네, **지금이 딱 JWT(JSON Web Token)를 도입해야 할 타이밍입니다.**

현재 올려주신 코드 상태를 분석해보면, JWT 도입이 필요한 이유는 다음과 같습니다.

1. **REST API 구조:** `BoardApplication`은 View(JSP/Thymeleaf)가 아닌 JSON을 반환하는 REST API 서버 형태입니다. 이 경우 세션/쿠키 방식보다는 **Stateless(무상태)** 한 토큰 인증 방식이 표준입니다.
2. **구현 단계:** 회원가입(`register`)은 구현되었고, `AuthController`에 `login`과 `refresh` 부분이 주석으로 남겨져 있습니다. 이제 "로그인 -> 토큰 발급 -> 토큰으로 게시글 작성"의 흐름을 만들 차례입니다.
3. **Spring Security 설정:** 현재 `SecurityConfig`가 `.permitAll()`로 모든 문을 열어두고 있습니다. 이를 닫고 인증된 사용자만 `Post`나 `Comment`를 작성("POST", "PUT", "DELETE")할 수 있게 막아야 합니다.

다음은 현재 프로젝트 구조(`com.elecbrandy.board`)에 맞춰 JWT를 도입하는 단계별 가이드입니다.

---

### 1. `build.gradle` 의존성 추가

JWT 생성을 위한 라이브러리(`jjwt`)를 추가합니다.

```groovy
dependencies {
    // ... 기존 의존성 ...
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
}

```

### 2. `application.yaml` 설정 추가

JWT 서명에 사용할 비밀키(Secret Key)를 설정합니다. (실제 운영 시에는 환경변수로 관리해야 합니다.)

```yaml
jwt:
  secret: c2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQtc2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQK # 임의의 긴 문자열 (Base64 인코딩 권장)

```

### 3. `JwtTokenProvider` 생성

`global/jwt` 패키지를 새로 만들고, 토큰 생성 및 검증 로직을 담당할 클래스를 만듭니다.

**파일 경로:** `src/main/java/com/elecbrandy/board/global/jwt/JwtTokenProvider.java`

```java
package com.elecbrandy.board.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 유저 정보를 가지고 AccessToken, RefreshToken 을 생성하는 메서드
    public TokenInfo generateToken(Authentication authentication) {
        // 권한 가져오기
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();

        // Access Token 생성 (30분 유효)
        Date accessTokenExpiresIn = new Date(now + 1800000); 
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token 생성 (옵션)
        String refreshToken = Jwts.builder()
                .setExpiration(new Date(now + 86400000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴
        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // 토큰 정보를 검증하는 메서드
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}

```

*(참고: `TokenInfo` DTO는 별도로 `dto` 패키지에 만들어주셔야 합니다.)*

### 4. `JwtAuthenticationFilter` 생성

요청이 들어올 때 헤더에서 토큰을 꺼내 검증하고, 유효하면 SecurityContext에 저장하는 필터입니다.

**파일 경로:** `src/main/java/com/elecbrandy/board/global/jwt/JwtAuthenticationFilter.java`

```java
package com.elecbrandy.board.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 1. Request Header 에서 JWT 토큰 추출
        String token = resolveToken((HttpServletRequest) request);

        // 2. validateToken 으로 토큰 유효성 검사
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 토큰이 유효할 경우 토큰에서 Authentication 객체를 가지고 와서 SecurityContext 에 저장
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    // Request Header 에서 토큰 정보 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

```

### 5. `SecurityConfig` 수정 (핵심)

이제 필터를 등록하고, 세션을 끄고(Stateless), API 권한을 설정합니다.

```java
// src/main/java/com/elecbrandy/board/global/config/SecurityConfig.java

@Configuration
@EnableWebSecurity // 추가
@RequiredArgsConstructor // 추가
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    // ... passwordEncoder 빈은 그대로 유지 ...

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 미사용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // 로그인, 회원가입은 누구나
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll() // 게시글 조회는 누구나
                .anyRequest().authenticated() // 그 외(글쓰기 등)는 인증 필요
            )
            // JWT 필터를 UsernamePasswordAuthenticationFilter 전에 추가
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}

```

### 결론 및 다음 단계

이제 **로그인 서비스 로직**(`UserService.login`)을 구현하여 이메일/비밀번호 검증 후 `JwtTokenProvider.generateToken()`을 호출해 토큰을 반환하도록 만들면 됩니다.


