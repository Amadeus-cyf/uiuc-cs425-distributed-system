#!/bin/sh
wget --load-cookies /tmp/cookies.txt "https://docs.google.com/uc?export=download&confirm=$(wget --quiet --save-cookies /tmp/cookies.txt --keep-session-cookies --no-check-certificate 'https://docs.google.com/uc?export=download&id=1amfztsxLzTqVbOA2Bk0fJR64sz9ChII7' -O- | sed -rn 's/.*confirm=([0-9A-Za-z_]+).*/\1\n/p')&id=1amfztsxLzTqVbOA2Bk0fJR64sz9ChII7" -O ../../voting.txt && rm -rf /tmp/cookies.txt 


wget 'https://docs.google.com/uc?export=download&id=1Fe3pmHgwEjlVTmFMKrvjuI_0ulTQUseM' -O ../../building_NYC.txt 


