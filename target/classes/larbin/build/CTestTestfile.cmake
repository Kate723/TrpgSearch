# CMake generated Testfile for 
# Source directory: /home/user/trpgsearch/src/main/resources/larbin
# Build directory: /home/user/trpgsearch/src/main/resources/larbin/build
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(larbin_project "larbin" "-c" "larbin-test.conf")
set_tests_properties(larbin_project PROPERTIES  _BACKTRACE_TRIPLES "/home/user/trpgsearch/src/main/resources/larbin/CMakeLists.txt;16;ADD_TEST;/home/user/trpgsearch/src/main/resources/larbin/CMakeLists.txt;0;")
subdirs("adns")
subdirs("src")
