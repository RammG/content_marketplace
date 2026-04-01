package com.tianzige.marketplace.graphql.pagination;

import graphql.relay.*;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ConnectionUtils {

    private static final String CURSOR_PREFIX = "cursor:";

    public static <T> Connection<T> toConnection(Page<T> page) {
        List<Edge<T>> edges = new ArrayList<>();
        int offset = page.getNumber() * page.getSize();

        for (int i = 0; i < page.getContent().size(); i++) {
            T node = page.getContent().get(i);
            String cursor = encodeCursor(offset + i);
            edges.add(new DefaultEdge<>(node, new DefaultConnectionCursor(cursor)));
        }

        ConnectionCursor startCursor = edges.isEmpty() ? null : edges.get(0).getCursor();
        ConnectionCursor endCursor = edges.isEmpty() ? null : edges.get(edges.size() - 1).getCursor();

        PageInfo pageInfo = new DefaultPageInfo(
                startCursor,
                endCursor,
                page.hasPrevious(),
                page.hasNext()
        );

        return new DefaultConnection<>(edges, pageInfo);
    }

    public static <T> ConnectionWithCount<T> toConnectionWithCount(Page<T> page) {
        Connection<T> connection = toConnection(page);
        return new ConnectionWithCount<>(
                connection.getEdges(),
                connection.getPageInfo(),
                page.getTotalElements()
        );
    }

    public static String encodeCursor(int offset) {
        return Base64.getEncoder().encodeToString((CURSOR_PREFIX + offset).getBytes());
    }

    public static int decodeCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            if (decoded.startsWith(CURSOR_PREFIX)) {
                return Integer.parseInt(decoded.substring(CURSOR_PREFIX.length()));
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getPageNumber(String cursor, int pageSize) {
        int offset = decodeCursor(cursor);
        return offset / pageSize;
    }

    public static class ConnectionWithCount<T> implements Connection<T> {
        private final List<Edge<T>> edges;
        private final PageInfo pageInfo;
        private final long totalCount;

        public ConnectionWithCount(List<Edge<T>> edges, PageInfo pageInfo, long totalCount) {
            this.edges = edges;
            this.pageInfo = pageInfo;
            this.totalCount = totalCount;
        }

        @Override
        public List<Edge<T>> getEdges() {
            return edges;
        }

        @Override
        public PageInfo getPageInfo() {
            return pageInfo;
        }

        public long getTotalCount() {
            return totalCount;
        }
    }
}
