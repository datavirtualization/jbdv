#########################################################################################
# This script helps maintain the langpacks/shortcuts                                    #
#                                                                                       #
# LANGPACK                                                                              #
# Compares the langpacks with each other to see if they are in sync                     #
#                                                                                       #
# SHORTCUT                                                                              #
# Compares the different shortcut langpacks to see if there are any differences.        #
# Expected differences are name/description to be different. Anything else is reported  #
#                                                                                       #
# UNUSEDID                                                                              #
# Checks our installer setup and izpack source (granted one is provided)                #
# to find if it can justify any of the ids we have declared in our source               #
# It reports any that it finds are not being used anywhere                              #
#########################################################################################

# Common variables
pathToLangPacks="../local-izpack/bin/langpacks/installer/"

if [[ $1 == "LANGPACK" ]]; then

   langpacks=(`ls $pathToLangPacks*.xml | xargs -n1 basename | sed "s/.xml//"`)

   echo Parsing : Start

   let cc=0
   for lang in "${langpacks[@]}" ;
   do
      echo Parsing : $lang.xml
      for i in `cat $pathToLangPacks$lang.xml` ;
      do
         id=`echo $i | grep "id=\"" | sed 's/id=//;s/"//g'`
         if [ ! -z "$id" ]; then
            out[$cc]=${out[$cc]}$id" "
         fi
      done
      let cc+=1
   done

   echo Parsing : Done
   echo Diff : Start

   # Cycle through all langpacks
   for i in "${!out[@]}"
   do
      # Cycle through current langpacks ids
      for j in `echo -e ${out[$i]}`
      do
         # Cycle through other langpacks (excluding the current picked one)
         for k in "${!out[@]}"
         do
            if [[ $k -eq $i ]]; then
               continue
            fi
            contains=`echo ${out[$k]} | grep $j | sed 's/id=//;s/"//g'`
            if [[ -z "$contains" ]]; then
               echo -e "\033[1m$j\033[0m was found in \033[1m${langpacks[$i]}\033[0m but not in \033[1m${langpacks[$k]}\033[0m"
               missing=true
            fi
         done
      done
   done

   if [ -z $missing ]; then
      echo All langpacks in sync
   fi

   echo Diff : Done

elif [[ $1 == "SHORTCUT" ]]; then

   # Compare unix shortcuts
   pathToDefault="../unix_shortcut_specification.xml"
   pathToOthers="../local-izpack/unix_shortcut/"

   if [[ ! -d $pathToOthers ]]; then
      echo
      echo Unix shortcut dir not found
      echo
   else
      packs=`ls $pathToOthers*.xml | sed "s/.xml//"`

      for j in $packs;
      do
         out=`diff -y --suppress-common-lines $pathToDefault $j.xml | grep -iv name | grep -iv description`
         if [ ! -z "$out" ]; then
            printf "%-61s %-10s\n\n" "unix_shortcut_specification.xml" "$j.xml"
            echo "$out"
            echo
         fi
      done
   fi

   # Compare windows shortcuts
   pathToDefault="../default_shortcut_specification.xml"
   pathToOthers="../local-izpack/default_shortcut/"

   if [[ ! -d $pathToOthers ]]; then
      echo Windows shortcut dir not found
   else
      packs=`ls $pathToOthers*.xml | sed "s/.xml//"`

      for j in $packs;
      do
         out=`diff -y --suppress-common-lines $pathToDefault $j.xml | grep -iv name | grep -iv description`
         if [ ! -z "$out" ]; then
            printf "%-61s %-10s\n\n" "default_shortcut_specification.xml" "$j.xml"
            echo "$out"
            echo
         fi
      done
   fi

elif [[ $1 == "UNUSEDID" ]]; then

   PATH_TO_INSTALLER=../../../
   PATH_TO_IZPACK="$2"

   if [[ -z $PATH_TO_IZPACK || ! -d $PATH_TO_IZPACK ]]; then
      echo IZPACK PATH NOT FOUND
      exit
   fi

   echo The following ids dont appear to be used:

   # Parse core-packs.xml
   for i in `cat $PATH_TO_INSTALLER/installer/eap-config/core-packs.xml`
   do
      if [[ "$i" == *id=\"* ]]; then
         id=`echo $i | sed "s/id=//" | sed 's/"//g'`
         coreids=${coreids}"\n"$id
      fi
   done

   # Parse install.xml
   for i in `cat $PATH_TO_INSTALLER/installer/eap-config/install.xml`
   do
      if [[ "$i" == *classname=\"* && "$i" == *Panel* ]]; then
         class=`echo $i | sed "s/classname=//" | sed 's/"//g'`
         usedpanels=${usedpanels}"\n"$class
      fi
   done

   for i in `cat ${pathToLangPacks}eng.xml`
   do

      if [[ "$i" == *id=\"* ]]; then

         # format/extract the id properly
         id=`echo $i | sed "s/id=//" | sed 's/"//g'`

         # reset iterative variable
         unset found

         # STAGE 1 : Check other xmls for used ids
         out1=$(grep $id $PATH_TO_INSTALLER/installer/eap-config/*.xml)

         if [[ -z $out1 ]]; then

            # STAGE 2 : Check our custom validators..etc for used ids
            out2=$(grep -r $id $PATH_TO_INSTALLER/installer/eap-config/sources)

            if [[ -z $out2 ]]; then

               # count # of periods in id
               count=$((`echo $id | sed 's/[^.]//g'|wc -m` -1))

               # STAGE 3 : If id is of the form panel.id then search for getI18nStringForClass("id", "panel")
               # otherwise just grep for the id in izpack
               if [ $count -eq 1 ]; then
                  first=$(echo $id | awk -F. '{print $1}')
                  second=$(echo $id | awk -F. '{print $2}')
                  # Check to see if first part of the id is a class name
                  if [[ ! -z $(find "$PATH_TO_IZPACK" -name "$first.java") ]]; then
                     # Check if panel is used
                     for p in `echo -e $usedpanels`
                     do
                        if [[ $p == *$first* ]]; then
                           found=true
                           break
                        fi
                     done
                  fi
                  if [ ! $found ]; then
                     out3=$(grep -r -e "\"$second\",.*\"$first\"" -e $id $PATH_TO_IZPACK)
                  fi
               else
                  out3=$(grep -r $id $PATH_TO_IZPACK)
               fi

               # STAGE 4 : Check if its being used in core-packs
               if [[ -z $out3 ]]; then
                  for s in `echo -e $coreids`
                  do
                     if [[ $id == *$s* ]]; then
                        found=true
                        break
                     fi
                  done
                  if [ ! $found ]; then
                     echo $id
                  fi
               fi
            fi
         fi
      fi
   done

else
   selfName=`basename $0 | sed "s/.sh//"`
   echo $selfName : missing operand
   echo ----------------------------------------------------------------
   echo Usage : $selfName TYPE ARGS
   echo ----------------------------------------------------------------
   echo Types :
   echo -e LANGPACK'\t'- Check whether langpacks are insync
   echo -e SHORTCUT'\t'- Check whether shortcuts are insync
   echo -e UNUSEDID'\t'- Check whether langpacks contain any unused ids
   echo Args :
   echo "<path to izpack directory> - Only used for UNUSEDIDS"
   echo ----------------------------------------------------------------
fi

