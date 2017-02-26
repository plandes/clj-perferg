## makefile automates the build and deployment for lein projects

# application name
APP_SCR_NAME=	pp2erg
REL_DIST=	$(REL_ZIP)

# location of the http://github.com/plandes/clj-zenbuild cloned directory
ZBHOME ?=	../clj-zenbuild

all:		info

include $(ZBHOME)/src/mk/compile.mk
include $(ZBHOME)/src/mk/dist.mk
include $(ZBHOME)/src/mk/release.mk

.PHONY: test
test:
	$(LEIN) test
