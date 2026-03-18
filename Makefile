# 변수 설정
GRADLE 			= ./gradlew
DOCKER_COMPOSE 	= docker-compose
CONTAINER_NAME 	= boilerplate-db
DB_NAME 		= boilerplate
DB_USER 		= myuser

# 색상 (출력 가독성용)
CYAN 	= \033[0;36m
GREEN 	= \033[0;32m
RESET 	= \033[0m

include .env
export

.PHONY: all up down run stop build test clean fclean re logs psql help

# 기본 명령어 (make 입력 시 실행) -> DB 켜고 앱 실행
all: up run

# 1. 실행 관련 (Execution)
up: ## Docker Compose로 DB 컨테이너 실행 (백그라운드)
	@echo "$(CYAN)>>> Starting Database Container...$(RESET)"
	$(DOCKER_COMPOSE) up -d

down: ## Docker Compose 종료
	@echo "$(CYAN)>>> Stopping Containers...$(RESET)"
	$(DOCKER_COMPOSE) down

run: ## Spring Boot 애플리케이션 실행 (bootRun)
	@echo "$(GREEN)>>> Running Spring Boot Application...$(RESET)"
	set -a && . ./.env && set +a && $(GRADLE) bootRun

stop: ## 실행 중인 Spring Boot 애플리케이션 정지 (Gradle --stop)
	@echo "$(CYAN)>>> Stopping Spring Boot Application...$(RESET)"
	$(GRADLE) --stop

# 2. 빌드 및 테스트 (Build & Test)
build: ## 프로젝트 빌드 (테스트 제외, 속도 최적화)
	@echo "$(GREEN)>>> Building Project (Skip Tests)...$(RESET)"
	$(GRADLE) build -x test

test: ## 테스트 코드 전체 실행
	@echo "$(GREEN)>>> Running Tests...$(RESET)"
	$(GRADLE) test

# 3. 청소 및 초기화 (Cleaning)
clean: ## DB 컨테이너 종료 및 볼륨 데이터 삭제 (DB 초기화)
	@echo "$(CYAN)>>> Cleaning Docker Environment (Removing Volumes)...$(RESET)"
	$(DOCKER_COMPOSE) down -v

fclean: clean
	@echo "$(CYAN)>>> Full Cleaning (Gradle Clean + DB Data)...$(RESET)"
	$(GRADLE) clean
	rm -rf ./db_data

re: fclean all ## 완전 초기화 후 다시 실행 (Re-build & Run)

# 4. 유틸리티 (Utils)
logs: ## DB 컨테이너 로그 확인
	$(DOCKER_COMPOSE) logs -f

psql: ## 실행 중인 DB 컨테이너에 접속 (psql)
	@echo "$(GREEN)>>> Connecting to PostgreSQL...$(RESET)"
	docker exec -it $(CONTAINER_NAME) psql -U $(DB_USER) -d $(DB_NAME)

help: ## 명령어 목록 보기
	@echo "$(CYAN)Available Commands:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-15s$(RESET) %s\n", $$1, $$2}'