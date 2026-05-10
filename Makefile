.PHONY: run run-dev stop build test

run:
	mvn spring-boot:run -pl bootstrap

run-dev:
	mvn spring-boot:run -pl bootstrap -Dspring.profiles.active=dev

stop:
	@-pkill -f "spring-boot" 2>/dev/null || true

build:
	mvn clean package -DskipTests

test:
	mvn clean test
