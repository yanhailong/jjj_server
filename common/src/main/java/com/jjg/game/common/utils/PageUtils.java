package com.jjg.game.common.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页工具类
 */
public class PageUtils {

    /**
     * 分页对象
     */
    public static class PageResult<T> {

        /**
         * 当前页码
         */
        private int pageIndex;

        /**
         * 每页条数
         */
        private int pageSize;

        /**
         * 总条数
         */
        private int totalCount;

        /**
         * 最大页数
         */
        private int maxPageIndex;

        /**
         * 分页后的数据
         */
        private List<T> data;

        public PageResult(int pageIndex, int pageSize, List<T> data) {
            this.pageIndex = pageIndex;
            this.pageSize = pageSize;
            this.data = data;
            page();
        }

        private void page() {
            if (pageSize <= 0) {
                pageSize = 20;
            }
            if (pageIndex <= 0) {
                pageIndex = 1;
            }
            if (data == null) {
                data = new ArrayList<>();
                return;
            }
            int totalCount = data.size();
            int totalPage = (totalCount + pageSize - 1) / pageSize;
            int startIndex = (pageIndex - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalCount);
            List<T> result = data.subList(startIndex, endIndex);
            setTotalCount(totalCount);
            setMaxPageIndex(totalPage);
            setData(result);
            setPageIndex(pageIndex);
            setPageSize(pageSize);
        }

        public int getPageIndex() {
            return pageIndex;
        }

        public void setPageIndex(int pageIndex) {
            this.pageIndex = pageIndex;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getMaxPageIndex() {
            return maxPageIndex;
        }

        public void setMaxPageIndex(int maxPageIndex) {
            this.maxPageIndex = maxPageIndex;
        }

        public List<T> getData() {
            return data;
        }

        public void setData(List<T> data) {
            this.data = data;
        }
    }

    public static <T> PageResult<T> page(List<T> data, int pageIndex, int pageSize) {
        return new PageResult<>(pageIndex, pageSize, data);
    }

}
