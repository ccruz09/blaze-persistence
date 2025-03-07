/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Blazebit
 */

package com.blazebit.persistence.examples.spring.hateoas.controller;

import com.blazebit.persistence.examples.spring.hateoas.filter.Filter;
import com.blazebit.persistence.examples.spring.hateoas.model.Cat;
import com.blazebit.persistence.examples.spring.hateoas.repository.CatRepository;
import com.blazebit.persistence.examples.spring.hateoas.repository.CatViewRepository;
import com.blazebit.persistence.examples.spring.hateoas.view.CatCreateView;
import com.blazebit.persistence.examples.spring.hateoas.view.CatUpdateView;
import com.blazebit.persistence.examples.spring.hateoas.view.CatWithOwnerView;
import com.blazebit.persistence.spring.data.repository.KeysetAwarePage;
import com.blazebit.persistence.spring.data.repository.KeysetPageable;
import com.blazebit.persistence.spring.data.webmvc.KeysetConfig;
import com.blazebit.persistence.spring.hateoas.webmvc.KeysetAwarePagedResourcesAssembler;
import com.blazebit.text.FormatUtils;
import com.blazebit.text.ParserContext;
import com.blazebit.text.SerializableFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Beikov
 * @since 1.5.0
 */
@RestController
public class CatRestController {

    private static final Map<String, SerializableFormat<?>> FILTER_ATTRIBUTES;

    static {
        Map<String, SerializableFormat<?>> filterAttributes = new HashMap<>();
        filterAttributes.put("id", FormatUtils.getAvailableFormatters().get(Long.class));
        filterAttributes.put("name", FormatUtils.getAvailableFormatters().get(String.class));
        filterAttributes.put("age", FormatUtils.getAvailableFormatters().get(Integer.class));
        filterAttributes.put("owner.name", FormatUtils.getAvailableFormatters().get(String.class));
        FILTER_ATTRIBUTES = Collections.unmodifiableMap(filterAttributes);
    }

    @Autowired
    private CatRepository catRepository;
    @Autowired
    private CatViewRepository catViewRepository;

    @RequestMapping(path = "/cats", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createCat(@RequestBody CatCreateView catCreateView) {
        catViewRepository.save(catCreateView);

        return ResponseEntity.ok(catCreateView.getId().toString());
    }

    @RequestMapping(path = "/cats/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateCat(@PathVariable("id") long id, @RequestBody CatUpdateView catUpdateView) {
        catViewRepository.save(catUpdateView);

        return ResponseEntity.ok(catUpdateView.getId().toString());
    }

    @RequestMapping(path = "/cats", method = RequestMethod.GET)
    public HttpEntity<Page<Cat>> findPaginated(
            @KeysetConfig(Cat.class) KeysetPageable keysetPageable,
            @RequestParam(name = "filter", required = false) final Filter[] filter,
            KeysetAwarePagedResourcesAssembler<Cat> assembler) {
        Specification<Cat> specification = getSpecificationForFilter(filter);

        Page<Cat> resultPage = catRepository.findAll(specification, keysetPageable);
        if (keysetPageable.getPageNumber() > resultPage.getTotalPages()) {
            throw new RuntimeException("Invalid page number!");
        }

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        for (Link link : assembler.toModel(resultPage).getLinks()) {
            if (link.getRel() == IanaLinkRelations.FIRST || link.getRel() == IanaLinkRelations.PREV || link.getRel() == IanaLinkRelations.NEXT || link.getRel() == IanaLinkRelations.LAST) {
                headers.add(HttpHeaders.LINK, link.toString());
            }
        }

        return new HttpEntity<>(resultPage, headers);
    }

    @RequestMapping(path = "/cats", method = RequestMethod.GET, produces = { "application/hal+json" })
    public PagedModel<EntityModel<Cat>> findPaginatedHateoas(
            @KeysetConfig(Cat.class) KeysetPageable keysetPageable,
            @RequestParam(name = "filter", required = false) final Filter[] filter,
            KeysetAwarePagedResourcesAssembler<Cat> assembler) {
        Specification<Cat> specification = getSpecificationForFilter(filter);

        Page<Cat> resultPage = catRepository.findAll(specification, keysetPageable);
        if (keysetPageable.getPageNumber() > resultPage.getTotalPages()) {
            throw new RuntimeException("Invalid page number!");
        }

        return assembler.toModel(resultPage);
    }

    @RequestMapping(path = "/cat-views", method = RequestMethod.GET)
    public HttpEntity<Page<CatWithOwnerView>> findPaginatedViews(
            @KeysetConfig(Cat.class) KeysetPageable keysetPageable,
            @RequestParam(name = "filter", required = false) final Filter[] filter,
            KeysetAwarePagedResourcesAssembler<CatWithOwnerView> assembler) {
        Specification<Cat> specification = getSpecificationForFilter(filter);

        Page<CatWithOwnerView> resultPage = catViewRepository.findAll(specification, keysetPageable, "test");
        if (keysetPageable.getPageNumber() > resultPage.getTotalPages()) {
            throw new RuntimeException("Invalid page number!");
        }

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        for (Link link : assembler.toModel(resultPage).getLinks()) {
            if (link.getRel() == IanaLinkRelations.FIRST || link.getRel() == IanaLinkRelations.PREV || link.getRel() == IanaLinkRelations.NEXT || link.getRel() == IanaLinkRelations.LAST) {
                headers.add(HttpHeaders.LINK, link.toString());
            }
        }

        return new HttpEntity<>(resultPage, headers);
    }

    @RequestMapping(path = "/cat-views", method = RequestMethod.GET, produces = { "application/hal+json" })
    public PagedModel<EntityModel<CatWithOwnerView>> findPaginatedViewsHateoas(
            @KeysetConfig(Cat.class) KeysetPageable keysetPageable,
            @RequestParam(name = "filter", required = false) final Filter[] filter,
            KeysetAwarePagedResourcesAssembler<CatWithOwnerView> assembler) {
        Specification<Cat> specification = getSpecificationForFilter(filter);

        KeysetAwarePage<CatWithOwnerView> resultPage = catViewRepository.findAll(specification, keysetPageable);
        if (keysetPageable.getPageNumber() > resultPage.getTotalPages()) {
            throw new RuntimeException("Invalid page number!");
        }
        return assembler.toModel(resultPage);
    }

    private Specification<Cat> getSpecificationForFilter(final Filter[] filter) {
        if (filter == null || filter.length == 0) {
            return null;
        }
        return new Specification<Cat>() {
            @Override
            public Predicate toPredicate(Root<Cat> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                ParserContext parserContext = new ParserContextImpl();
                try {
                    for (Filter f : filter) {
                        SerializableFormat<?> format = FILTER_ATTRIBUTES.get(f.getField());
                        if (format != null) {
                            String[] fieldParts = f.getField().split("\\.");
                            Path<?> path = root.get(fieldParts[0]);
                            for (int i = 1; i < fieldParts.length; i++) {
                                path = path.get(fieldParts[i]);
                            }
                            switch (f.getKind()) {
                                case EQ:
                                    predicates.add(criteriaBuilder.equal(path, format.parse(f.getValue(), parserContext)));
                                    break;
                                case GT:
                                    predicates.add(criteriaBuilder.greaterThan((Expression<Comparable>) path, (Comparable) format.parse(f.getValue(), parserContext)));
                                    break;
                                case LT:
                                    predicates.add(criteriaBuilder.lessThan((Expression<Comparable>) path, (Comparable) format.parse(f.getValue(), parserContext)));
                                    break;
                                case GTE:
                                    predicates.add(criteriaBuilder.greaterThanOrEqualTo((Expression<Comparable>) path, (Comparable) format.parse(f.getValue(), parserContext)));
                                    break;
                                case LTE:
                                    predicates.add(criteriaBuilder.lessThanOrEqualTo((Expression<Comparable>) path, (Comparable) format.parse(f.getValue(), parserContext)));
                                    break;
                                case IN:
                                    List<String> values = f.getValues();
                                    List<Object> filterValues = new ArrayList<>(values.size());
                                    for (String value : values) {
                                        filterValues.add(format.parse(value, parserContext));
                                    }
                                    predicates.add(path.in(filterValues));
                                    break;
                                case BETWEEN:
                                    predicates.add(criteriaBuilder.between((Expression<Comparable>) path, (Comparable) format.parse(f.getLow(), parserContext), (Comparable) format.parse(f.getHigh(), parserContext)));
                                    break;
                                case STARTS_WITH:
                                    predicates.add(criteriaBuilder.like((Expression<String>) path, format.parse(f.getValue(), parserContext) + "%"));
                                    break;
                                case ENDS_WITH:
                                    predicates.add(criteriaBuilder.like((Expression<String>) path, "%" + format.parse(f.getValue(), parserContext)));
                                    break;
                                case CONTAINS:
                                    predicates.add(criteriaBuilder.like((Expression<String>) path, "%" + format.parse(f.getValue(), parserContext) + "%"));
                                    break;
                                default:
                                    throw new UnsupportedOperationException("Unsupported kind: " + f.getKind());
                            }
                        }
                    }
                } catch (ParseException ex) {
                    throw new RuntimeException(ex);
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        };
    }

    private static class ParserContextImpl implements ParserContext {
        private final Map<String, Object> contextMap;

        private ParserContextImpl() {
            this.contextMap = new HashMap();
        }

        public Object getAttribute(String name) {
            return this.contextMap.get(name);
        }

        public void setAttribute(String name, Object value) {
            this.contextMap.put(name, value);
        }
    }
}
