# Spring-boot Boiler plate

Spring-boot 기반 프로젝트 시 초기 설정을 빠르게 하기 위한 목적의 Boilerplate 입니다.
`branch` 별로 다양한 상황을 구분해서, 사이드 프로젝트에 쉽게 가져다 쓸 수 있도록 관리할 예정입니다.

## Branch 별 기능 소개

- `user-register-jwt`
  - _httpOnly cookie + jwt_ 토큰을 통한 Register, Login 기능


## How to use

| 명령어            | 하는 일        | 비고 |
|----------------|-------------|-|
| `make  up`     | DB만 실행      | |
| `make  run`    | 스프링 부트 앱 실행 | `./gradlew bootRun` |
| `make all`     | DB + 앱 실행   | |
| `make  psql`   | DB 콘솔 접속    | |
| `make  down`   | DB 종료       | |
| `make  clean`  | DB 데이터 초기화 | |
| `make  fclean` | DB 삭제 + 빌드파일 삭제 | 프로젝트 초기화 |
| `make  re`     | 싹 지우고 다시 실행 | |

## Documentation
- **Swagger**: 외부 API 명세용 (Controller, DTO에 적용)
- **JavaDoc**: 내부 로직 설명용 (Service, Repository에 적용)
