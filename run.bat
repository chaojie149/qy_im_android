for /f "tokens=* delims=" %%n in ('dir "%cd%" /b/ad/s') do (

echo "%%n"

if exist "%%n\.keep" (del /f /s /q "%%n\.keep")||(echo NotFile)

dir/a/b "%%n\"|findstr . >nul&&(echo NotEmpty)||(echo.>"%%n\.keep")

)