package org.lerot.myELH;

import java.io.File;

// An ELH of the open project: its name, the file it is saved to and its top
// node. For the ELH being edited, top is the tree in memory, unsaved edits and all.
public record ProjectElh(String name, File file, ElhNode top)
{
}
