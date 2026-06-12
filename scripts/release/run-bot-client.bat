@echo off
setlocal
set "APP_HOME=%~dp0.."
java %JAVA_OPTS% -cp "%APP_HOME%\lib\*" dev.krotname.networkchat.client.BotChatClient %*
