# Fabricweight

Fabricweight is a personal tool to create git patch based forks of projects. It is not intended to be used by others, but feel free to do whatever you want with it(keeping in mind the MIT license).

It support forking of forks. In the case of Minecraft mods, this means the 1.16.5 branch can be a fork of the 1.17.1 branch, which can be a fork of the 1.18.2 branch(and so on).

This project is inspired by [paperweight](https://github.com/PaperMC/paperweight) and spigots buildtools. But these tools are way too complicated for my needs, and too specific to their project.

## Usage

Create a file called ``repo`` with the git url of the project you want to fork. Then create a second file called ``sha`` with the commit hash of the commit you want to fork from. Added to that, an empty directory called ``patches``. Running ``java -jar fabricweight.jar patch`` will download the project, and setup a ``workspace`` folder. Inside this folder, you can edit the code as you want. When you are done, commit your changed, and run ``java -jar fabricweight.jar rb`` to create the patch files in the ``patches`` folder. At any later point(after changing the ``sha`` for example), you can run ``java -jar fabricweight.jar patch`` again to update the ``workspace`` folder to the new commit. ``workspace`` and ``upstream`` should be in your ``.gitignore``.
