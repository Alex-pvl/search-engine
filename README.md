# search-engine
Сервис для парсинга, индексирования и поиска.

### Запуск локально
- Клонируем репозиторий:
```shell
git clone https://github.com/alex-pvl/search-engine.git
```
- Создаем БД
```sql
CREATE ROLE search_engine WITH SUPERUSER LOGIN PASSWORD 'search_engine';
CREATE DATABASE search_engine OWNER search_engine;
```
- Запускаем проект
```shell
chmod +x ./gradlew
./gradlew build
./gradlew run
```
- Дергаем эндпоинт `/crawler/start` в файле `requests.http`
- Записываем статистические данные в json файл через ручку /crawler/json
- Запускаем python-скрипт для визуализации результатов
```shell
cd src/main/resources
python3 stats.py
```