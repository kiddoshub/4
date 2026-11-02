package com.gaspoweredcake.client33;

import com.gaspoweredcake.client33.gui.utils.WorkspacePage;
import java.util.HashSet;

public final class WorkspacePageRegression {
    public static void main(String[] args) { run(); }
    public static void run() {
        for(int width:new int[]{240,620,1100})for(int height:new int[]{90,300,800}) {
            var first=WorkspacePage.layout(width,height,174,0);
            var seen=new HashSet<Integer>();
            for(int page=0;page<first.pages();page++) {
                var layout=WorkspacePage.layout(width,height,174,page);
                for(int i=layout.from();i<layout.to();i++)if(!seen.add(i))throw new AssertionError("Duplicate module across pages");
            }
            if(seen.size()!=174)throw new AssertionError("Pagination hid modules");
        }
        if(WorkspacePage.layout(620,300,1,99).page()!=0)throw new AssertionError("Search must clamp stale pages");
        var empty=WorkspacePage.layout(200,100,0,-2);
        if(empty.from()!=0 || empty.to()!=0 || empty.pages()!=1)throw new AssertionError("Empty library must remain navigable");
        for (int width : new int[] {160, 480, 800}) for (int height : new int[] {74, 274, 414}) {
            var first = WorkspacePage.strata(width, height, 174, 0);
            var seen = new HashSet<Integer>();
            for (int page = 0; page < first.pages(); page++) {
                var layout = WorkspacePage.strata(width, height, 174, page);
                for (int i = layout.from(); i < layout.to(); i++)
                    if (!seen.add(i)) throw new AssertionError("Duplicate Strata module");
            }
            if (seen.size() != 174) throw new AssertionError("Strata pagination hid modules");
        }
        if (WorkspacePage.strata(800, 414, 174, 0).size() != 24)
            throw new AssertionError("Strata GUI units must show six rows of four readable cards");
        System.out.println("Workspace pagination regressions passed.");
    }
}
