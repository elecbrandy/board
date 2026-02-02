up:
	docker-compose up -d

down:
	docker-compose down

psql:
	docker exec -it board-db psql -U myuser -d board