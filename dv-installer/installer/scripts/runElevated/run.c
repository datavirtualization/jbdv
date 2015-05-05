#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main ( int argc, char *argv[] )
{
   int i;
   char clean[argc*300];
   strcpy(clean,"");
   for (i = 1;i<argc;i++){
      if (containsWhiteSpace(argv[i])==1) {
		strcat(clean,"\"");
		strcat(clean,argv[i]);
		strcat(clean,"\"");
	  } else {
		strcat(clean,argv[i]);
	  }
	  strcat(clean," ");
   }
   system(clean);
   return 0;
}

int containsWhiteSpace(char *str) {
	int length = strlen(str);
	int cc = 0;
	while (cc < length) {
		if (str[cc] == ' ') {
			return 1;
		}
		cc++;
	}
	return 0;
}
