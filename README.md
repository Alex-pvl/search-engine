# search-engine
Сервис для парсинга, индексирования и поиска.

### Запуск локально
1. Клонируем репозиторий:
```shell
git clone https://github.com/alex-pvl/search-engine.git
```
2. Создаем БД
```sql
CREATE ROLE search_engine WITH SUPERUSER LOGIN PASSWORD 'search_engine';
CREATE DATABASE search_engine OWNER search_engine;
```
3. Запускаем проект
```shell
chmod +x ./gradlew
./gradlew build
./gradlew run
```
