@echo off

:initForGradle
echo "Creating development environment setup..."
goto initDefault

:initDefault
call git submodule init
call git submodule update
goto complete

:complete
echo "...setup complete."
goto exit

:exit