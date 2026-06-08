package com.uniondesk.common.web;

import java.util.List;

public record PageResult<T>(long total, List<T> list) {
}
