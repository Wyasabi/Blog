package blog_iovasofta.controller;


import blog_iovasofta.bindingModel.ArticleBindingModel;
import blog_iovasofta.entity.Article;
import blog_iovasofta.entity.Category;
import blog_iovasofta.entity.Tag;
import blog_iovasofta.entity.User;
import blog_iovasofta.repository.ArticleRepository;
import blog_iovasofta.repository.CategoryRepository;
import blog_iovasofta.repository.TagRepository;
import blog_iovasofta.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @GetMapping("/article/create")
    @PreAuthorize("isAuthenticated()")
    public String create (Model model){
        List<Category> categories = this.categoryRepository.findAll();
        model.addAttribute("categories", categories);
    model.addAttribute("view", "article/create");

    return "base-layout";
    }

    @PostMapping("/article/create")
    @PreAuthorize("isAuthenticated()")
    public String createProcess(ArticleBindingModel articleBindingModel){
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());

        Category category = this.categoryRepository.getOne(articleBindingModel.getCategoryId());

        HashSet<Tag> tags = this.findTagsFromString(articleBindingModel.getTagString());
        Article articleEntity = new Article(articleBindingModel.getTitle(), articleBindingModel.getContent(), userEntity, category, tags);

        this.articleRepository.saveAndFlush(articleEntity);

        return "redirect:/";
    }

    @GetMapping("/article/{id}")
    public String details(Model model, @PathVariable Integer id){
        if(!this.articleRepository.existsById(id)){ //existById e promeneno ot originala
            return "redirect:/";
        }
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
            UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            User entityUser = this.userRepository.findByEmail(principal.getUsername());

            model.addAttribute("user", entityUser);
        }
            Article article = this.articleRepository.getOne(id); // findOne e orginalnoto}

            model.addAttribute("article", article);
            model.addAttribute("view", "article/details");

            return "base-layout";

        }

    @GetMapping("/article/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String edit(@PathVariable Integer id, Model model){

        if(!this.articleRepository.existsById(id)){
            return "redirect:/";
        }
        Article article = this.articleRepository.getOne(id);

        if(!isUserAuthorOrAdmin(article)){
            return "redirect:/article/" + id;
        }


     //   String tagString = article.getTags().stream().map(Tag::getName).collect(Collectors.joining(", "));
        List<Category> categories = this.categoryRepository.findAll();
        model.addAttribute("view", "article/edit");
        model.addAttribute("article", article);
        model.addAttribute("categories", categories);
       // model.addAttribute("tags", tagString);

        return "base-layout";
    }


    @PostMapping("/article/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editProcess(@PathVariable Integer id, ArticleBindingModel articleBindingModel){
    if(!this.articleRepository.existsById(id)){
        return "redirect:/";
    }
        Article article = this.articleRepository.getOne(id);
        if(!isUserAuthorOrAdmin(article)){
            return "redirect:/article/" + id;
        }

       // HashSet<Tag> tags = this.findTagsFromString(articleBindingModel.getTagString());
        Category category = this.categoryRepository.getOne(articleBindingModel.getCategoryId());

        article.setContent(articleBindingModel.getContent());
        article.setTitle(articleBindingModel.getTitle());
        article.setCategory(category);
      //  article.setTags(tags);


        this.articleRepository.saveAndFlush(article);

        return "redirect:/article/" + article.getId();

    }

    @GetMapping("/article/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete(Model model, @PathVariable Integer id){
        if(!this.articleRepository.existsById(id)){
            return "redirect:/";
        }
            Article article = this.articleRepository.getOne(id);
        if(!isUserAuthorOrAdmin(article)){
            return "redirect:/article/" + id;
        }
            model.addAttribute("article", article);
            model.addAttribute("view", "article/delete");

            return "base-layout";

    }

    @PostMapping("/article/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deleteProcess(@PathVariable Integer id){
        if(!this.articleRepository.existsById(id)){
            return "redirect:/";
        }
            Article article = this.articleRepository.getOne(id);
        if(!isUserAuthorOrAdmin(article)){
            return "redirect:/article/" + id;
        }
            this.articleRepository.delete(article);

            return "redirect:/";

    }

    private boolean isUserAuthorOrAdmin(Article article){
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        User userEntity = this.userRepository.findByEmail(user.getUsername());

        return userEntity.isAdmin() || userEntity.isAuthor(article);
    }

    private HashSet<Tag> findTagsFromString(String tagString){
        HashSet<Tag> tags = new HashSet<>();
        String[] tagNames = tagString.split(",\\s*");

        for (String tagName : tagNames){
            Tag currentTag = this.tagRepository.findByName(tagName);

            if(currentTag ==null){
                currentTag = new Tag(tagName);
                this.tagRepository.saveAndFlush(currentTag);
            }
            tags.add(currentTag);
        }
        return tags;
    }

}
