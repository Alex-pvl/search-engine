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
4. Дергаем эндпоинт `/crawler/start` в файле `requests.http`
5. Записываем статистические данные в json файл через ручку `/crawler/json`
6. Запускаем python-скрипт для визуализации результатов
```shell
cd src/main/resources
python3 stats.py
```
---
### Альтернативный мониторинг
5. Переходим на ветку `monitoring`
6. Поднимаем прометеус с графаной
7. Добавляем дашборду с метриками паука:
- `crawler_urls_count`
- `crawler_words_count`
- `crawler_wordLocations_count`
- `crawler_links_count`
- `crawler_linkWords_count`
