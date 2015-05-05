SUBDIRS := spec shortcuts
TMP := $(shell mktemp -t --directory)
CWD := $(shell pwd)


# These variables are to convert Java-style locale IDs to the locale IDs.
# Used for localized release notes URLs in function sedLocalizeDocsURL:
docsurl_de := de-DE
docsurl_es := es-ES
docsurl_fr := fr-FR
docsurl_ja := ja-JP
docsurl_pt-BR := pt-BR
docsurl_zh-CN := zh-CN

# make intltool happy:
top_srcdir = ..

# Maps iso2 filenames to iso3 filenames.
# Used to convert PO dir names to langpack xml filenames
langpack_de := deu
langpack_es := spa
langpack_fr := fra
langpack_ja := jpn
langpack_pt-BR := por
langpack_zh-CN := chn
langpack_xxx := xxx

JBIROOT = ..
LANGPATH = $(JBIROOT)/langpacks
UNIXSHORTCUTPATH = $(JBIROOT)/unix_shortcut
WINDOWSSHORTCUTPATH = $(JBIROOT)/windows_shortcut
JBILTOOLS = $(CWD)/tools/jbiltools/jbiltools
TMP := $(shell mktemp -d)


.PHONY : clean help langpacks pot merge potmerge xml pseudo spec/pot spec/merge spec/xml

help:
	@echo "make templates   generates translation templates (POT), merges with current translations (PO)"
	@echo "make pseudo      generates pseudo-translation xxx.po from POT files"
	@echo ""
	@echo "make langpacks   generates localized files in ../local-izpack from POs"
	@echo ""
	@echo "make clean       removes temporary files, including xxx/*.po"

templates: pot merge

langpacks: xml spec/xml

clean:
	rm -rf xxx/
	rm -f ../local-izpack/bin/langpacks/installer/xxx.xml ../local-izpack/unix_shortcut/xxx.xml
	rm -f default_shortcut_specification.xml.in unix_shortcut_specification.xml.in POTFILES.in

# Generate annotated XML files from the shortcut spec files.  intltool can 
# then extract text from them. These files don't *have* to be in version
# control, but keeping them there will help to avoid unnecessary changes 
# to timestamps in POT/PO files.
default_shortcut_specification.xml.in unix_shortcut_specification.xml.in: %.xml.in: $(UNIXSHORTCUTPATH)/en_default.xml $(WINDOWSSHORTCUTPATH)/en_default.xml
	cat $< | sed 's/name=/_name=/g;s/defaultName=/_defaultName=/g;s/description=/_description=/g' > $@

# generates pot/*.pot
pot: pot/jbil.pot pot/shortcuts.pot

strings.txt: pot/jbil.pot
	grep msgid pot/jbil.pot | awk '{print $$2}' >strings.txt

#pot/jbil.pot : $(JBIROOT)/install.xml $(LANGPATH)/eng.xml $(JBIROOT)/userInputSpec.xml
pot/jbil.pot : $(LANGPATH)/eng.xml
	@# Patches description error.
	#thauser: this seems un-needed. i have modified the rule to not need the other xmls, because when i commented the eng.xml out, the jbil.pot contained no strings for translation.
#	sed -i 's/<description><\/description>/<description> <\/description>/g' $(JBIROOT)/install.xml
	@# Extracts translatable strings.
	touch $(TMP)/extracted_eng.xml
#       $(JBILTOOLS) --type=im --if=$(JBIROOT)/install.xml >> $(TMP)/extracted_eng.xml
#	$(JBILTOOLS) --type=im --if=$(JBIROOT)/install-jbpm.xml >> $(TMP)/extracted_eng.xml
#	$(JBILTOOLS) --type=im --if=$(JBIROOT)/install-jbossrules.xml >> $(TMP)/extracted_eng.xml
	$(JBILTOOLS) --type=lm --if=$(LANGPATH)/eng.xml >> $(TMP)/extracted_eng.xml
#	$(JBILTOOLS) --type=um --if=$(JBIROOT)/userInputSpec.xml >> $(TMP)/extracted_eng.xml

#	@# Removes duplications. - no need!  "jbiltools --type=m" will merge duplicate entries
	#cat $(TMP)/extracted_eng.xml | sort > $(TMP)/extracted_eng_sort.xml
	#mv $(TMP)/extracted_eng_sort.xml $(TMP)/extracted_eng.xml

	@# Adds header and footer to make langpack XML well-formed.
	$(JBILTOOLS) --type=h > $(TMP)/main_eng.xml
	cat $(TMP)/extracted_eng.xml >> $(TMP)/main_eng.xml
	$(JBILTOOLS) --type=f >> $(TMP)/main_eng.xml
	@# Converts from main langpack xml file to pot file.
	mkdir -p pot/
	$(JBILTOOLS) --type=m --if=$(TMP)/main_eng.xml > pot/jbil.pot

# update shortcuts POT from annotated XML files
pot/shortcuts.pot: unix_shortcut_specification.xml.in default_shortcut_specification.xml.in
	mkdir -p pot
	rm -f POTFILES.in; touch POTFILES.in
	intltool-update --maintain 2>/dev/null
	cat missing >> POTFILES.in; rm missing
	intltool-update --pot --gettext-package=pot/shortcuts

# generates xxx.po from POT files
pseudo: xxx xxx/jbil.po xxx/shortcuts.po

xxx :
	mkdir xxx

xxx/jbil.po : pot/jbil.pot
	# we don't want en.po in this directory, lest it break
	# the targets which glob *.po (xml, merge)
	msginit --locale=en --no-translator --input=pot/jbil.pot --output-file=$(TMP)/en.po
	podebug --progress none --format '[xxx] ' --rewrite=xxx --input $(TMP)/en.po --output xxx/jbil.po
	rm -f $(TMP)/en.po

xxx/shortcuts.po: pot/shortcuts.pot
	msginit --locale=en --no-translator --input pot/shortcuts.pot --output-file=$(TMP)/en.po
	podebug --progress none --format '[xxx] ' --rewrite=xxx --input $(TMP)/en.po --output xxx/shortcuts.po
	rm -f $(TMP)/en.po

# Merges translation progress in PO with latest string specs in POT.
merge:
	$(foreach po, $(wildcard */*.po), \
		msgmerge --quiet $(po) pot/$(notdir $(po))t > $(TMP)/merged.po; \
		mv -f $(TMP)/merged.po $(po);)

# replace en-US with another locale in docs URLs
define sedLocalizeDocsURL
	sed -e "s|redhat.com/docs/en-US|redhat.com/docs/$(docsurl_$(1))|g"
endef

# Generates localized versions of ../local-izpack/*_shortcut_specification.xml from any available PO files
xml: unix_shortcut_specification.xml.in default_shortcut_specification.xml.in
	mkdir $(TMP)/xml $(TMP)/shortcuts
	# make a copy of shortuct po files in gettext layout for intltool
	$(foreach po, $(wildcard */shortcuts.po), \
		cp $(po) $(TMP)/shortcuts/$(shell basename $(dir $(po))).po; \
	)
	# merge translations from PO into shortcut spec files
	cd $(TMP)/xml; \
		intltool-merge --xml-style --multiple-output $(TMP)/shortcuts $(CWD)/default_shortcut_specification.xml.in default_shortcut_specification.xml; \
		intltool-merge --xml-style --multiple-output $(TMP)/shortcuts $(CWD)/unix_shortcut_specification.xml.in unix_shortcut_specification.xml
	#	mkdir -p ../local-izpack/default_shortcut ../local-izpack/unix_shortcut
	# for each locale, update docs url in shortcut spec files
	$(foreach locdir, $(dir $(wildcard */shortcuts.po)), \
		$(call sedLocalizeDocsURL,$(shell basename $(locdir))) $(TMP)/xml/$(locdir)default_shortcut_specification.xml > $(WINDOWSSHORTCUTPATH)/$(shell basename $(locdir)).xml; \
		$(call sedLocalizeDocsURL,$(shell basename $(locdir))) $(TMP)/xml/$(locdir)unix_shortcut_specification.xml > $(UNIXSHORTCUTPATH)/$(shell basename $(locdir)).xml; \
	)
	rm -fr $(TMP)/xml $(TMP)/shortcuts

# Converts from translated po file to main langpack xml file.
spec/xml :
	$(foreach po, $(wildcard */jbil.po), $(JBILTOOLS) --type=p --if=$(po) > \
		$(LANGPATH)/$(langpack_$(shell basename $(dir $(po)))).xml;)

