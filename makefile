# ============================================
# Makefile для языка Kirka
# ============================================

# Переменные
JAVAC       = javac
JAVA        = java
JAR         = jar
JAR_NAME    = kirka.jar
MAIN_CLASS  = com.main.kirka
SRC_DIR     = com/main
SOURCE      = $(SRC_DIR)/kirka.java
CLASSES     = $(SRC_DIR)/*.class
SCRIPT_FILE ?= program.kirka   # файл для запуска (можно переопределить)

# Цель по умолчанию – сборка JAR
all: jar

# Компиляция исходников
compile:
	$(JAVAC) -d . $(SOURCE)

# Создание исполняемого JAR
jar: compile
	$(JAR) cfe $(JAR_NAME) $(MAIN_CLASS) $(CLASSES)

# Очистка .class файлов и JAR
clean:
	rm -f $(CLASSES) $(JAR_NAME)

# Глубокая очистка (все class-файлы в пакете)
clean-all:
	rm -rf $(SRC_DIR)/*.class $(JAR_NAME)

# Утилита: показать текущие настройки
info:
	@echo "JAR_NAME = $(JAR_NAME)"
	@echo "MAIN_CLASS = $(MAIN_CLASS)"
	@echo "SOURCE = $(SOURCE)"
	@echo "SCRIPT_FILE = $(SCRIPT_FILE)"

# Объявляем цели, которые не являются файлами
.PHONY: all compile jar run run-no-args clean clean-all info