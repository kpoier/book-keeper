@echo off
chcp 65001 > nul
echo sending POST request to http://localhost:8080/api/login ...
echo ----------------------------------------------------

curl -s -X POST http://localhost:8080/api/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\": \"thomas\", \"password\": \"mypassword123\"}" | python -m json.tool

echo.
echo ----------------------------------------------------
echo finished
pause
