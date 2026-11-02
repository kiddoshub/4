package com.gaspoweredcake.client33.gui.utils;

/** Responsive page sizing preserves readable controls instead of shrinking text to fit. */
public record WorkspacePage(int columns,int size,int page,int pages,int from,int to) {
    public static WorkspacePage layout(double width,double height,int count,int requestedPage) {
        return layout(width, height, count, requestedPage, 300, 88, 3, 6);
    }

    /** Strata dimensions are GUI units, while the classic workspace uses device pixels. */
    public static WorkspacePage strata(double width, double height, int count, int requestedPage) {
        return layout(width, height, count, requestedPage, 170, 64, 4, 8);
    }

    private static WorkspacePage layout(double width, double height, int count, int requestedPage,
                                         int cardWidth, int rowHeight, int maxColumns, int maxRows) {
        int columns=Math.max(1,Math.min(maxColumns,(int)(width/cardWidth)));
        int rows=Math.max(1,Math.min(maxRows,(int)(height/rowHeight)));
        int size=columns*rows;
        int pages=Math.max(1,(count+size-1)/size);
        int page=Math.max(0,Math.min(pages-1,requestedPage));
        int from=Math.min(count,page*size);
        return new WorkspacePage(columns,size,page,pages,from,Math.min(count,from+size));
    }
}
