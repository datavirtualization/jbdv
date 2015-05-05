# The ModeShape RHQ Plugin

This is the Git repository used for the productized version of the ModeShape RHQ plugin.

## Git Productization Overview

Read this [document](https://docspace.corp.redhat.com/docs/DOC-150303) for a detailed explanation of the process.

## Committing fixes and features

Before trying to commit any changes to this repository, you need to make sure you have the appropriate access (see the above document).

Once you have access perform the following steps:

  in your community ModeShape RHQ repository fork add a new remote pointing to this repository:

        $ git remote add prod git@github.com:jboss-integration/modeshape-rhq.git

  create your own private fork of this repository, rename it to jboss-integration.modeshape-rhq and then clone your fork:

        $ git clone git@github.com:<you>/jboss-integration.modeshape-rhq.git
        $ cd jboss-integration.modeshape-rhq.git
        $ git remote add upstream git@github.com:jboss-integration/modeshape-rhq.git

### Fixes and features that have already been included in the community repository

If you want to include a bug fix or a new feature that has already been reviewed and committed in [public repository](https://github.com/ModeShape/modeshape-rhq),
perform the following steps:

        select your public modeshape-rhq repository fork

  switch to the branch where you want to add the commit or create a local tracking branch of the remote branch:

        $ git checkout -b <branch_name> prod/<branch_name> or
        $ git checkout <branch_name>

  cherry pick each of the commits that make up the fix/feature onto this branch:

        $ git cherry-pick <commit_id>

  make sure the codebase compiles and the tests successfully pass by running:

        $ mvn clean install -s settings.xml

  push the changes to the remote branch:

        $ git push prod <branch_name>

### Fixes and features that are not present in the community repository

    select your private fork of the jboss-integration/modeshape-rhq.git repository (see above)

  create a new working branch:

       $ git checkout -b <topic_branch_name>

  after you're happy with your changes and a full build (with unit tests) runs successfully, commit your changes on your
  topic branch:

       $ git checkout <destination_branch>               # switches to the branch where your changes should be merged
       $ git pull upstream <destination_branch>          # fetches all 'upstream' changes and merges 'upstream/<destination_branch>'
                                                         onto your '<destination_branch>'
       $ git checkout <topic_branch_name>                # switches to your topic branch
       $ git rebase <destination_branch>                 # reapplies your changes on top of the latest in <destination_branch>
       	                                                 (i.e., the latest from master will be the new base for your changes)
  push your topic branch onto your private fork:

      $ git push origin <topic_branch_name>              # pushes your topic branch into your private fork of ModeShape

  [generate a pull-request](http://help.github.com/pull-requests/) for your changes.