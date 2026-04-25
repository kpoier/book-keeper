@echo off
chcp 65001 > nul
echo sending POST request to http://localhost:8080/api/records ...
echo ----------------------------------------------------

curl -s -X POST http://localhost:8080/api/records ^
  -H "Content-Type: application/json" ^
  -d "{\"amount\": 150.5, \"category\": \"Food\", \"type\": \"expense\", \"date\": \"2026-04-22\", \"note\": \"Lunch test\"}" | python -m json.tool

echo.
echo ----------------------------------------------------
echo finished
pause
